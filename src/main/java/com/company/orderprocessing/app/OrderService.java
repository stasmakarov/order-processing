package com.company.orderprocessing.app;

import com.company.orderprocessing.record.OrderRecord;
import com.company.orderprocessing.entity.*;
import com.company.orderprocessing.event.IncomingOrderEvent;
import com.company.orderprocessing.nominatim.GeoCodingService;
import com.company.orderprocessing.rabbit.InventoryMessageProducer;
import com.company.orderprocessing.util.Iso8601Converter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.appsettings.AppSettings;
import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.UiEventPublisher;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component(value = "ord_OrderService")
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);


    private final static String START_MESSAGE_NAME = "Start order processing";
    private final Random random = new Random();

    @Autowired
    private SystemAuthenticator systemAuthenticator;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private NumeratorService numeratorService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private GeoCodingService geoCodingService;
    @Autowired
    private UiEventPublisher uiEventPublisher;
    @Autowired
    private AppSettings appSettings;
    @Autowired
    private InventoryMessageProducer inventoryMessageProducer;


    public void startOrderProcess(String json) {
        OrderRecord orderRecord = deserialize(json);
        if (orderRecord == null) return;

        Integer maxDelayTimer = appSettings.load(OrderProcessingSettings.class).getMaxDelayTimer();
        int timerNew = random.nextInt(1, maxDelayTimer);
        int timerVerified = random.nextInt(1, maxDelayTimer);
        String timerNewDuration = Iso8601Converter.convertSecondsToDuration(timerNew);
        String timerVerifiedDuration = Iso8601Converter.convertSecondsToDuration(timerVerified);

        String businessKey = generateBusinessKey();
        Item item = findItemByName(orderRecord.item());

        Map<String, Object> map = new HashMap<>();
        map.put("orderNumber", businessKey);
        map.put("customer", orderRecord.customer());
        map.put("address", orderRecord.address());
        map.put("quantity", orderRecord.quantity());
        map.put("item", item);
        map.put("timerNew", timerNewDuration);
        map.put("timerVerified", timerVerifiedDuration);

        systemAuthenticator.begin("admin");
        try {
            runtimeService.startProcessInstanceByMessage(START_MESSAGE_NAME,
                    businessKey,
                    map);
            log.info("Order process started");
            uiEventPublisher.publishEvent(new IncomingOrderEvent(this, json));
        } catch (Exception e) {
            //noinspection JmixRuntimeException
            throw new RuntimeException(e);
        } finally {
            systemAuthenticator.end();
        }
    }

    private String generateBusinessKey() {
        Integer number = numeratorService.getNext("order");
        String formattedNumber = String.format("%05d", number);
        return "ORD-" + formattedNumber;
    }

    public OrderRecord deserialize(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, OrderRecord.class);
        } catch (JsonProcessingException e) {
            log.error("Bad message: {}", json);
            return null;
        }
    }

    private Item findItemByName(String name) {
        systemAuthenticator.begin("admin");
        try {
            return dataManager.load(Item.class)
                    .query("select e from ord_Item e where e.name = :name")
                    .parameter("name", name)
                    .one();
        } catch (Exception ignored) {
        } finally {
            systemAuthenticator.end();
        }
        return null;
    }

    public boolean simulatePayment(Order order) {
        MDC.put("Order #", order.getNumber());
        if (randomError()) {
            log.error("Payment failed");
            return false;
        }
        log.info("Payment proceeded");
        return true;
    }

    public boolean simulateCancelPayment(Order order) {
        MDC.put("Order #", order.getNumber());
        log.info("Payment cancelled");
        return true;
    }


    private boolean randomError() {
        int randomValue = random.nextInt(1,100);
        Integer errorProbability = appSettings.load(OrderProcessingSettings.class)
                .getPaymentErrorProbability();
        return randomValue < errorProbability;
    }

    public Order createOrder(String customer,
                             String address,
                             Item item,
                             Integer quantity,
                             String orderNumber,
                             DelegateExecution execution) {
        systemAuthenticator.begin("admin");
        try {
            Order order = dataManager.create(Order.class);
            order.setCustomer(customer);
            order.setAddress(address);
            order.setItem(item);
            order.setQuantity(quantity);
            order.setProcessInstanceId(execution.getProcessInstanceId());
            order.setNumber(orderNumber);
            order.setStatus(OrderStatus.NEW);
            dataManager.save(order);
            MDC.put("Order #", order.getNumber());
            log.info("Order created: {}", order.getNumber());
            return order;
        } catch (IllegalArgumentException ignored) {
        } finally {
            systemAuthenticator.end();
        }
        return null;
    }

    public void setOrderStatus(Order order, int statusId) {
        OrderStatus status = OrderStatus.fromId(statusId);
        systemAuthenticator.begin("admin");
        try {
            order.setStatus(status);
            dataManager.save(order);
            log.info("Order #: {}, Status set: {}", order.getNumber(), status);
        } finally {
            systemAuthenticator.end();
        }
    }

    public void doReservation(Order order) {
        inventoryMessageProducer.sendInventoryMessage(order.getItem(), order.getQuantity(), ItemOperation.RESERVATION);

        log.info("Try reservation for order {}", order.getNumber());
    }

    public void cancelReservation(Order order) {
        inventoryMessageProducer.sendInventoryMessage(order.getItem(), order.getQuantity(), ItemOperation.CANCEL_RESERVATION);
        log.info("✅Cancelled reservation for order {}", order.getNumber());
    }

    public void confirmDelivery(Order order) {
        inventoryMessageProducer.sendInventoryMessage(order.getItem(), order.getQuantity(), ItemOperation.DELIVERY);
        log.info("✅Delivery completed for order {}", order.getNumber());
    }

    public void addressVerification(Order order) {
        String address = order.getAddress();
        Point point = geoCodingService.verifyAddress(address);
        if (point != null) {
            systemAuthenticator.begin();
            try {
                order.setStatus(OrderStatus.VERIFIED);
                order.setLocation(point);
                dataManager.save(order);
                log.info("Address verified {}", address);
            } finally {
                systemAuthenticator.end();
            }
        } else {
            throw new BpmnError("100");
        }
    }

    public void setLocation(Order order, Point point) {
        systemAuthenticator.begin("admin");
        try {
            order.setLocation(point);
            dataManager.save(order);
        } finally {
            systemAuthenticator.end();
        }
    }
}
