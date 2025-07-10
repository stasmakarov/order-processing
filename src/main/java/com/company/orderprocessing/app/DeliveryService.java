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
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static java.time.LocalDateTime.now;

@Component(value = "ord_DeliveryService")
public class DeliveryService {
    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);
    private  static final Random random = new Random();
    private static final String START_DELIVERY = "Start delivery";

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private NumeratorService numeratorService;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private SystemAuthenticator systemAuthenticator;
    @Autowired
    private AppSettings appSettings;

    @Transactional
    public List<Order> getOrdersWaitingDelivery() {
        systemAuthenticator.begin("admin");
        try {
            List<Order> orders = dataManager.load(Order.class)
                    .query("select e from ord_Order e where e.status = :status")
                    .parameter("status", OrderStatus.READY)
                    .list();
            log.info("Getting orders for delivery");
            return orders != null ? orders : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error while retrieving orders for delivery", e);
            return Collections.emptyList();
        } finally {
            systemAuthenticator.end();
        }
    }

    @Transactional
    public String createDelivery(List<Order> orders) {
        systemAuthenticator.begin("admin");
        try {
            SaveContext saveContext = new SaveContext();
            Delivery delivery = dataManager.create(Delivery.class);
            delivery.setNumber("DEL-" + numeratorService.getNext("delivery").toString());
            delivery.setTimestamp(now());
            saveContext.saving(delivery);
            dataManager.save(saveContext);
            return delivery.getNumber();
        } finally {
            systemAuthenticator.end();
        }
    }

    public void startDeliveryProcess() {
        Integer maxDelayTimer = appSettings.load(OrderProcessingSettings.class).getMaxDelayTimer();
        int randomValue = random.nextInt(10, maxDelayTimer);
        Map<String, Object> params = new HashMap<>();
        params.put("deliveryTimer", Iso8601Converter.convertSecondsToDuration(randomValue));
        runtimeService.startProcessInstanceByMessage(START_DELIVERY, params);
        log.info("Delivery process started");
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
        Integer maxDelayTimer = appSettings.load(OrderProcessingSettings.class).getMaxDelayTimer();
        int delay = random.nextInt(10, maxDelayTimer);
        try {
            Thread.sleep(1000L * delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Delivery has completed: " + deliveryNumber);
    }

    @Transactional
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

    public void sendMessage(Order order, String messageName) {
        String processInstanceId = order.getProcessInstanceId();
        systemAuthenticator.begin("admin");
        try {
            if (processInstanceId != null) {
                EventSubscription subscription = null;
                try {
                    subscription = runtimeService.createEventSubscriptionQuery()
                            .processInstanceId(processInstanceId)
                            .eventName(messageName)
                            .singleResult();
                } catch (Exception e) {
                    log.info("Error fetching subscription for message {},  {}", messageName, order.getNumber());
                    return;
                }

                if (subscription == null) return;
                String executionId = subscription.getExecutionId();
                log.info("Message sent: {} to {}", messageName, order.getNumber());

                if (executionId != null) {
                    runtimeService.messageEventReceived(messageName, executionId);
                } else {
                    log.error("Subscription ID: {} - Execution ID is NULL, message can not be send",
                            subscription.getId());
                }
            }
        } finally {
            systemAuthenticator.end();
        }
    }
}