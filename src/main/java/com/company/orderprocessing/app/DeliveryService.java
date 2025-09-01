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
import org.flowable.engine.runtime.ProcessInstance;
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


    public void startDeliveryProcess() {
        List<UUID> orderIds;
        systemAuthenticator.begin("admin");
        try {
            orderIds = dataManager.loadValue(
                            "select e.id from ord_Order e where e.status = :status", UUID.class)
                    .parameter("status", OrderStatus.READY)
                    .list();
        } finally {
            systemAuthenticator.end();
        }

        if (orderIds.isEmpty()) {
            log.warn("No orders ready for delivery");
            return;
        }
        Integer maxDelayTimer = appSettings.load(OrderProcessingSettings.class).getMaxDelayTimer();
        int randomValue = random.nextInt(10, maxDelayTimer);

        String businessKey = "DEL" + numeratorService.getNext("delivery").toString();
        Map<String, Object> params = new HashMap<>();
        params.put("deliveryTimer", Iso8601Converter.convertSecondsToDuration(randomValue));
        params.put("orderIds", orderIds);

        ProcessInstance instance = runtimeService.startProcessInstanceByMessage(START_DELIVERY, businessKey, params);
        if (instance != null) {
            log.info("🚚 Delivery process started");
        } else {
            log.error("🤷🏻‍♂️ Delivery process failed");
        }
    }

    @Transactional
    public List<Order> createDelivery(List<UUID> orderIds, DelegateExecution execution) {

        systemAuthenticator.begin("admin");
        try {
            var orders = dataManager.load(com.company.orderprocessing.entity.Order.class)
                    .query("select e from ord_Order e where e.id in :ids")
                    .parameter("ids", orderIds)
                    .list();

            if (orders.isEmpty()) {
                log.warn("No orders found for ids: {}", orderIds);
                return null;
            }

            SaveContext saveContext = new SaveContext();

            Delivery delivery = dataManager.create(Delivery.class);
            delivery.setTimestamp(java.time.LocalDateTime.now());
            delivery.setProcessInstanceId(execution.getProcessInstanceId());
            delivery.setNumber(execution.getProcessInstanceBusinessKey());

            for (Order order : orders) {
                order.setDelivery(delivery);
                saveContext.saving(order);
            }

            saveContext.saving(delivery);
            dataManager.save(saveContext);

            log.info("Delivery #{} created, {} orders attached", delivery.getNumber(), orders.size());

            return orders;
        } finally {
            systemAuthenticator.end();
        }
    }

    public void performDelivery(DelegateExecution execution) {

        System.out.println("🛵 Delivery has completed: " + execution.getProcessInstanceBusinessKey());
    }

    public void sendMessage(Order order, String messageName) {
        String processInstanceId = order.getProcessInstanceId();
        systemAuthenticator.begin("admin");
        try {
            if (processInstanceId != null) {
                EventSubscription subscription;
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