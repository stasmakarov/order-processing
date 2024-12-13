package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.*;
import com.company.orderprocessing.nominatim.GeoCodingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

@Component(value = "ord_OrderService")
public class OrderService {

    private final String START_MESSAGE_NAME = "Start order processing";
    private final SystemAuthenticator systemAuthenticator;

    @Autowired
    private DataManager dataManager;
    @Autowired
    private RuntimeService runtimeService;

    public OrderService(SystemAuthenticator systemAuthenticator) {
        this.systemAuthenticator = systemAuthenticator;
    }

    public void startOrderProcess(String json) {
        OrderRecord orderRecord = deserialize(json);
        String businessKey = generateBusinessKey();
        Item item = findItemByName(orderRecord.item().name());

        Map<String, Object> map = new HashMap<>();
        map.put("orderNumber", businessKey);
        map.put("customer", orderRecord.customer());
        map.put("address", orderRecord.address());
        map.put("quantity", orderRecord.quantity());
        map.put("item", item);
        systemAuthenticator.begin();
        try {
            runtimeService.startProcessInstanceByMessage(START_MESSAGE_NAME, businessKey, map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            systemAuthenticator.end();
        }
    }

    private String generateBusinessKey() {
        String orderNumber = null;
        systemAuthenticator.begin();
            try {
                Numerator numerator = dataManager.load(Numerator.class)
                        .query("select e from ord_Numerator e where e.name = 'order'")
                        .one();
                orderNumber = ("ORD-" + String.format("%03d", numerator.getNumber()));
                numerator.setNumber(numerator.getNumber() + 1);
                dataManager.save(numerator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
                systemAuthenticator.end();
        }
        return orderNumber;
    }

    public OrderRecord deserialize(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, OrderRecord.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Item findItemByName(String name) {
        Item item = null;
        systemAuthenticator.begin();
        try {
            item = dataManager.load(Item.class)
                    .query("select e from ord_Item e where e.name = :name")
                    .parameter("name", name)
                    .one();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            systemAuthenticator.end();
        }
        return item;
    }

    public void simulatePayment() {
        randomDelay();
        if (randomError())
            throw new BpmnError("902");
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
        systemAuthenticator.begin();
        try {
            Order order = dataManager.create(Order.class);
            order.setCustomer(customer);
            order.setAddress(address);
            order.setItem(item);
            order.setQuantity(quantity);
            order.setStatus(OrderStatus.NEW);
            order.setProcessInstanceId(execution.getProcessInstanceId());
            order.setOrderNumber(orderNumber);
            dataManager.save(order);
            return order;
        } catch (Exception ignored) {}
        finally {
            systemAuthenticator.end();
        }
        return null;
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

        systemAuthenticator.begin();
        try {
            order.setStatus(status);
            dataManager.save(order);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            systemAuthenticator.end();
        }
    }

    public int reservation(Item item, Long reserve) {
        systemAuthenticator.begin();
        try {
            int intReserve = 0;
            try {
                intReserve = Math.toIntExact(reserve);
            } catch  (ArithmeticException ignored) {}

            int availableQty = item.getTotalQuantity() - item.getReserved();
            if (availableQty >= intReserve) {
                item.setReserved(item.getReserved() + intReserve);
                System.out.println("Reservation success: " + item.getName() + ": " + reserve);
                return 1;
            } else {
                System.out.println("Reservation failed: " + item.getName() + ": " + reserve);
                return 0;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            systemAuthenticator.end();
        }
    }

    public void canselReservation(Item item, Long reserve) {
        reservation(item, -reserve);
    }

    @Autowired
    private GeoCodingService geoCodingService;

    public void addressVerification(Order order) {
        String address = order.getAddress();
        Point point = geoCodingService.verifyAddress(address);
        systemAuthenticator.begin();
        try {
            if (point != null) {
                order.setStatus(OrderStatus.VERIFIED);
                order.setLocation(point);
                dataManager.save(order);
            } else {
                throw new BpmnError("100");
            }
        } catch (Exception ignored) {
        } finally {
            systemAuthenticator.end();
        }
    }
}
