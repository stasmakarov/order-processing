package com.company.orderprocessing.listener;

import com.company.orderprocessing.publisher.RequestEventPublisher;
import com.company.orderprocessing.event.RequestSendEvent;
import com.company.orderprocessing.nominatim.GeoCodingService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component(value = "ord_RequestEventsListener")
public class RequestEventsListener {

    @Autowired
    private GeoCodingService geoCodingService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RequestEventPublisher requestEventPublisher;

    @Async
    @EventListener
    public void onRequestSendEvent(RequestSendEvent event) {
        String address = event.getAddress();
        String processInstanceId = event.getProcessInstanceId();
        Point point = geoCodingService.verifyAddress(address);

        if (point != null) {
            requestEventPublisher.notifyAddressVerificationCompletedSuccess(address, processInstanceId);
            sendMessageToProcess("Address OK", processInstanceId);
        } else {
            requestEventPublisher.notifyAddressVerificationCompletedFailure(address, processInstanceId);
            sendMessageToProcess("Fake address", processInstanceId);
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
