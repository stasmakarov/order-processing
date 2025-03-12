package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.Delivery;
import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.entity.OrderStatus;
import com.company.orderprocessing.event.DeliveryCompletedEvent;
import com.company.orderprocessing.repository.DeliveryRepository;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.UiEventPublisher;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.eventsubscription.api.EventSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;

@Component(value = "ord_DeliveryService")
public class DeliveryService {
    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);
    private  static final Random random = new Random();
    private static final String MESSAGE_NAME = "Order delivered";

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private NumeratorService numeratorService;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private SystemAuthenticator systemAuthenticator;
    @Autowired
    private UiEventPublisher uiEventPublisher;
    @Autowired
    private DeliveryRepository deliveryRepository;

    public List<Order> getOrdersWaitingDelivery() {
        systemAuthenticator.begin("admin");
        try {
            List<Order> orders = dataManager.load(Order.class)
                    .query("select e from ord_Order e where e.status = :status")
                    .parameter("status", OrderStatus.READY)
                    .list();
            log.info("Getting orders for delivery");
            return orders;
        } finally {
            systemAuthenticator.end();
        }
    }

    public String createDelivery(List<Order> orders) {
        systemAuthenticator.begin("admin");
        try {
            SaveContext saveContext = new SaveContext();
            Delivery delivery = dataManager.create(Delivery.class);
            delivery.setNumber("DEL-" + numeratorService.getNext("delivery").toString());
            delivery.setTimestamp(now());
            saveContext.saving(delivery);
            for (Order order : orders) {
                order.setDelivery(delivery);
                order.setStatus(OrderStatus.IN_DELIVERY);
                saveContext.saving(order);
            }
            dataManager.save(saveContext);
            return delivery.getNumber();
        } finally {
            systemAuthenticator.end();
        }
    }

    public void setBusinessKey(String deliveryNumber, DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        systemAuthenticator.begin("admin");
        try {
            runtimeService.updateBusinessKey(processInstanceId, deliveryNumber);
        } finally {
            systemAuthenticator.end();
        }
    }

    /**
     * Performs delivery for the given delivery number.
     *
     * @param deliveryNumber the number of the delivery to perform
     */
    @Transactional
    public void performDelivery(String deliveryNumber) {
        systemAuthenticator.begin("admin");
        SaveContext saveContext = new SaveContext();
        Delivery delivery = getDelivery(deliveryNumber);
        if (delivery == null) return;

        List<Order> orders = delivery.getOrders();

        // Collect item IDs from orders
        List<UUID> itemIds = orders.stream()
                .map(order -> order.getItem().getId())
                .collect(Collectors.toList());
        // Load only relevant items
        List<Item> items = dataManager.load(Item.class)
                .query("select i from ord_Item i where i.id in :itemIds")
                .parameter("itemIds", itemIds)
                .list();

        Map<UUID, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));

        for (Order order : orders) {
            UUID itemId = order.getItem().getId();
            Item orderedItem = itemMap.get(itemId);
            if (orderedItem != null) {
                Integer quantity = order.getQuantity();
                orderedItem.setDelivered(orderedItem.getDelivered() + quantity);
                orderedItem.setReserved(orderedItem.getReserved() - quantity);
                if (orderedItem.getReserved() < 0) {
                    System.out.println("Reservation < 0");
                }
                saveContext.saving(orderedItem);
            }

            systemAuthenticator.begin("admin");
            try {
                dataManager.save(saveContext);
                uiEventPublisher.publishEvent(new DeliveryCompletedEvent(this, deliveryNumber));
                log.info("Delivery completed: {}", deliveryNumber);
            } finally {
                systemAuthenticator.end();
            }
        }
    }

    private Delivery getDelivery(String deliveryNumber) {
        Delivery delivery = null;
        systemAuthenticator.begin("admin");
        try {
            delivery = dataManager.load(Delivery.class)
                    .query("select e from ord_Delivery e where e.number = :number")
                    .parameter("number", deliveryNumber)
                    .one();
        } catch (Exception ignored) {
            log.error("Delivery not found for number: {}", deliveryNumber);
        } finally {
            systemAuthenticator.end();
        }
        return delivery;
    }


    public void sendMessage(Order order) {
        String processInstanceId = order.getProcessInstanceId();
        systemAuthenticator.begin("admin");
        try {
            if (processInstanceId != null) {
                List<EventSubscription> subscriptions = runtimeService.createEventSubscriptionQuery()
    //                    .processInstanceId(processInstanceId)
    //                    .eventName(MESSAGE_NAME)
                        .list();

                for (EventSubscription subscription : subscriptions) {
                    if (processInstanceId.equals(subscription.getProcessInstanceId())
                            && MESSAGE_NAME.equals(subscription.getEventName())) {
                        String executionId = subscription.getExecutionId();
                        if (executionId != null) {
                            runtimeService.messageEventReceived(MESSAGE_NAME, executionId);
                        } else {
                            log.error("Subscription ID: {} - Execution ID is NULL, message can not be send",
                                    subscription.getId());
                        }
                    }
                }
            }
        } finally {
            systemAuthenticator.end();
        }
    }
}