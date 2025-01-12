package com.company.orderprocessing.listener;

import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.publisher.RequestEventPublisher;
import com.company.orderprocessing.event.RequestSendEvent;
import com.company.orderprocessing.nominatim.GeoCodingService;
import com.company.orderprocessing.repository.OrderRepository;
import io.jmix.core.UnconstrainedDataManager;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component(value = "ord_RequestEventsListener")
public class RequestEventsListener {

    private static final Logger log = LoggerFactory.getLogger(RequestEventsListener.class);
    @Autowired
    private GeoCodingService geoCodingService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private UnconstrainedDataManager unconstrainedDataManager;

    @Async
    @EventListener
    public void onRequestSendEvent(RequestSendEvent event) {
        Order order = event.getOrder();
        String processInstanceId = event.getProcessInstanceId();
        Point point = geoCodingService.verifyAddress(order.getAddress());

        if (point != null) {
            order.setLocation(point);
            unconstrainedDataManager.save(order);
            sendMessageToProcess("Address OK", processInstanceId);
            log.info("Order # {}, Address verified: {}", order.getNumber(), order.getAddress());
        } else {
            sendMessageToProcess("Fake address", processInstanceId);
            log.info("Order # {}, Invalid address: {}", order.getNumber(), order.getAddress());
        }
    }

    private void sendMessageToProcess(String messageName, String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (processInstance == null) return;
        Execution execution = runtimeService.createExecutionQuery()
                .messageEventSubscriptionName(messageName)
                .parentId(processInstance.getId())
                .singleResult();
        if (execution == null) return;
        runtimeService.messageEventReceivedAsync(messageName, execution.getId());
    }
}
