package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.Order;
import org.flowable.engine.RuntimeService;
import org.flowable.eventsubscription.api.EventSubscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component(value = "ord_DeliveryService")
public class DeliveryService {
    @Autowired
    private RuntimeService runtimeService;

    private final String MESSAGE_NAME = "Delivery";

    public void sendMessage(Order order) {
        String processInstanceId = order.getProcessInstanceId();
        if (processInstanceId != null) {
            List<EventSubscription> subscriptions = runtimeService.createEventSubscriptionQuery()
                    .list();
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