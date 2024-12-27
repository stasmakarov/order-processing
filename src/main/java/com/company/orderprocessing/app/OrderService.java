package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.*;
import com.company.orderprocessing.nominatim.GeoCodingService;
import com.company.orderprocessing.repository.ItemRepository;
import com.company.orderprocessing.repository.NumeratorRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.security.SystemAuthenticator;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.ProcessInstance;
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


    private final String START_MESSAGE_NAME = "Start order processing";
    private final SystemAuthenticator systemAuthenticator;

    @Autowired
    private UnconstrainedDataManager unconstrainedDataManager;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private NumeratorRepository numeratorRepository;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private GeoCodingService geoCodingService;

    public OrderService(SystemAuthenticator systemAuthenticator) {
        this.systemAuthenticator = systemAuthenticator;
    }

    public void startOrderProcess(String json) {
        OrderRecord orderRecord = deserialize(json);
        if (orderRecord == null) return;

        String businessKey = generateBusinessKey();
        Item item = findItemByName(orderRecord.item());
        Map<String, Object> map = new HashMap<>();
        map.put("orderNumber", businessKey);
        map.put("customer", orderRecord.customer());
        map.put("address", orderRecord.address());
        map.put("quantity", orderRecord.quantity());
        map.put("item", item);
        systemAuthenticator.begin();
        try {
            ProcessInstance instance = runtimeService.startProcessInstanceByMessage(START_MESSAGE_NAME,
                    businessKey,
                    map);
            MDC.put("Process BK", instance.getBusinessKey());
            log.info("Order process started");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            systemAuthenticator.end();
        }
    }

    private String generateBusinessKey() {
        Integer value = numeratorRepository.getNextValue();
        return value.toString();
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
        Optional<Item> itemOptional = itemRepository.findByName(name);
        return itemOptional.orElse(null);
    }

    public boolean simulatePayment(Order order) {
        MDC.put("Order #", order.getOrderNumber());
        randomDelay();
        if (randomError()) {
            log.error("Payment failed");
            return false;
        }
        log.info("Payment proceeded");
        return true;
    }

    public boolean simulateCancelPayment(Order order) {
        MDC.put("Order #", order.getOrderNumber());
        log.info("Payment cancelled");
        return true;
    }

    private void randomDelay() {
        Random random = new Random();
        long i = random.nextLong(5L, 20L);
        try {
            Thread.sleep(i * 1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean randomError() {
        Random random = new Random();
        int i = random.nextInt(100);
        return i > 70;
    }

    public Order createOrder(String customer,
                             String address,
                             Item item,
                             Integer quantity,
                             String orderNumber,
                             DelegateExecution execution) {
        Order order = unconstrainedDataManager.create(Order.class);
        order.setCustomer(customer);
        order.setAddress(address);
        order.setItem(item);
        order.setQuantity(quantity);
        order.setStatus(OrderStatus.NEW);
        order.setProcessInstanceId(execution.getProcessInstanceId());
        order.setOrderNumber(orderNumber);
        unconstrainedDataManager.save(order);
        MDC.put("Order #", order.getOrderNumber());
        log.info("Order created");
        return order;
    }

    public void setOrderStatus(Order order, int statusId) {
        OrderStatus status = switch (statusId) {
            case 10 -> OrderStatus.NEW;
            case 20 -> OrderStatus.VERIFIED;
            case 30 -> OrderStatus.READY;
            case 40 -> OrderStatus.IN_DELIVERY;
            case 50 -> OrderStatus.COMPLETED;
            case 60 -> OrderStatus.CANCELLED;
            default -> null;
        };
        order.setStatus(status);
        unconstrainedDataManager.save(order);
        MDC.put("Order #", order.getOrderNumber());
        log.info("Status set: {}", status);
    }

    private boolean reservationGeneral(Order order, ReservationSign sign) {
        int direction;
        String message;
        if (ReservationSign.PLUS.equals(sign)) {
            direction = 1;
            message = "Reservation";
        } else {
            direction = -1;
            message = "Reservation canceling";
        }
        MDC.put("Order #", order.getOrderNumber());
        UUID id = order.getItem().getId();
        Optional<Item> itemOptional = itemRepository.findById(id);
        if (itemOptional.isEmpty()) return false;

        Item item = itemOptional.get();
        Integer reserve = order.getQuantity() * direction;

        int availableQty = item.getTotalQuantity() - item.getReserved();
        if (availableQty >= reserve) {
            item.setReserved(item.getReserved() + reserve);
            log.info("{} success: {}: {}", message, item.getName(), reserve);
            return true;
        } else {
            log.info("{} failed: {}: {}", message, item.getName(), reserve);
            return false;
        }
    }

    public boolean doReservation(Order order) {
        MDC.put("Order #", order.getOrderNumber());
        log.info("Doing reservation");
        return reservationGeneral(order, ReservationSign.PLUS);
    }

    public boolean cancelReservation(Order order) {
        MDC.put("Order #", order.getOrderNumber());
        log.info("Cancelling reservation");
        return reservationGeneral(order, ReservationSign.MINUS);
    }

    public void addressVerification(Order order) {
        MDC.put("Order #", order.getOrderNumber());
        String address = order.getAddress();
        Point point = geoCodingService.verifyAddress(address);
        if (point != null) {
            order.setStatus(OrderStatus.VERIFIED);
            order.setLocation(point);
            unconstrainedDataManager.save(order);
            log.info("Address verified {}", address);
        } else {
            throw new BpmnError("100");
        }
    }
}
