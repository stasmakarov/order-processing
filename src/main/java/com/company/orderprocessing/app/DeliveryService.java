package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.*;
import com.company.orderprocessing.repository.DeliveryRepository;
import com.company.orderprocessing.util.Iso8601Converter;
import io.jmix.appsettings.AppSettings;
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

import java.util.*;

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
    @Autowired
    private AppSettings appSettings;

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

    public void startDeliveryProcess() {
        Integer maxDelayTimer = appSettings.load(OrderProcessingSettings.class).getMaxDelayTimer();
        int randomValue = random.nextInt(1, maxDelayTimer);
        Map<String, Object> params = new HashMap<>();
        params.put("deliveryTimer", Iso8601Converter.convertSecondsToDuration(randomValue));
        runtimeService.startProcessInstanceByMessage(MESSAGE_NAME, params);
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

    public void performDelivery(String deliveryNumber) {
        System.out.println("Delivery has completed: " + deliveryNumber);
    }

    private Delivery getDeliveryByNumber(String deliveryNumber) {
        systemAuthenticator.begin("admin");
        try {
            return dataManager.load(Delivery.class)
                    .query("select e from ord_Delivery e where e.number = :number")
                    .parameter("number", deliveryNumber)
                    .one();
        } catch (Exception ignored) {
            log.error("Delivery not found for number: {}", deliveryNumber);
        } finally {
            systemAuthenticator.end();
        }
        return null;
    }


    public void sendMessageOrderDelivered(Order order) {
        String processInstanceId = order.getProcessInstanceId();
        systemAuthenticator.begin("admin");
        try {
            if (processInstanceId != null) {
                List<EventSubscription> subscriptions = runtimeService.createEventSubscriptionQuery()
                        .processInstanceId(processInstanceId)
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