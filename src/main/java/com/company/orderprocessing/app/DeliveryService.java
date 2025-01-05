package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.Delivery;
import com.company.orderprocessing.entity.Numerator;
import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.entity.OrderStatus;
import com.company.orderprocessing.event.DeliveryCompletedEvent;
import io.jmix.core.SaveContext;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.UiEventPublisher;
import org.flowable.engine.RuntimeService;
import org.flowable.eventsubscription.api.EventSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

import static java.time.LocalDateTime.now;

@Component(value = "ord_DeliveryService")
public class DeliveryService {
    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private NumeratorService numeratorService;
    @Autowired
    private UnconstrainedDataManager unconstrainedDataManager;
    @Autowired
    private SystemAuthenticator systemAuthenticator;
    @Autowired
    private UiEventPublisher uiEventPublisher;

    private final String MESSAGE_NAME = "Delivery";

    public List<Order> getOrderWaitingDelivery() {
        List<Order> orders = unconstrainedDataManager.load(Order.class)
                .query("select e from ord_Order e where e.status = :status")
                .parameter("status", OrderStatus.READY)
                .list();
        log.info("Getting orders for delivery");
        return orders;
    }

    public String createDelivery(List<Order> orders) {
        SaveContext saveContext = new SaveContext();
        Delivery delivery = unconstrainedDataManager.create(Delivery.class);
        delivery.setNumber("DEL-" + numeratorService.getNext("delivery").toString());
        delivery.setTimestamp(now());
        saveContext.saving(delivery);
        for (Order order : orders) {
            order.setDelivery(delivery);
            order.setStatus(OrderStatus.IN_DELIVERY);
            saveContext.saving(order);
        }
        unconstrainedDataManager.save(saveContext);
        return delivery.getNumber();
    }

    public void performDelivery (String deliveryNumber) {
        randomDelay();
        log.info("Delivery completed: {}", deliveryNumber);

        systemAuthenticator.begin();
        try {
            uiEventPublisher.publishEvent(new DeliveryCompletedEvent(this, deliveryNumber));
        } finally {
            systemAuthenticator.end();
        }
    }

    private void randomDelay() {
        Random random = new Random();
        long i = random.nextLong(3L, 12L);
        try {
            Thread.sleep(i * 1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(Order order) {
        String processInstanceId = order.getProcessInstanceId();
        if (processInstanceId != null) {
            List<EventSubscription> subscriptions = runtimeService.createEventSubscriptionQuery()
                    .processInstanceId(processInstanceId)
                    .eventName(MESSAGE_NAME)
                    .list();
            System.out.println(subscriptions);

            for (EventSubscription subscription : subscriptions) {
                if (subscription.getProcessInstanceId().equals(processInstanceId)
                        && subscription.getEventName().equals(MESSAGE_NAME)) {
                    String executionId = subscription.getExecutionId();
                    runtimeService.messageEventReceivedAsync(MESSAGE_NAME, executionId);
                }
            }
        }
    }
}