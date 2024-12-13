package com.company.orderprocessing.app;

import com.company.orderprocessing.event.RequestEventPublisher;
import com.company.orderprocessing.nominatim.GeoCodingService;
import io.jmix.flowui.backgroundtask.BackgroundTaskHandler;
import io.jmix.flowui.backgroundtask.BackgroundWorker;
import org.checkerframework.checker.units.qual.A;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.eventsubscription.api.EventSubscription;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.time.LocalTime.now;

@Component(value = "ord_AddressVerificator")
public class AddressVerificator {

    @Autowired
    private BackgroundWorker backgroundWorker;
    @Autowired
    private GeoCodingService geoCodingService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RequestEventPublisher requestEventPublisher;

    public void verify(String address, DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        System.out.println("Going to send request event at: " + now());
        requestEventPublisher.initiateAddressVerificationRequest(address, processInstanceId);
        System.out.println("Send request event at: " + now());

//        BackgroundVerificationTask task = new BackgroundVerificationTask(address,
//                processInstanceId,
//                geoCodingService,
//                runtimeService);
//
//        BackgroundTaskHandler<Void> taskHandler = backgroundWorker.handle(task);
//        taskHandler.execute();
    }

    public void verifySimple(String address, String processInstanceId) {
        Point point = geoCodingService.verifyAddress(address);
        boolean addressVerificationResult = (point != null);
        if (addressVerificationResult) {
            String executionId = getExecutionId("OK", processInstanceId);
            runtimeService.messageEventReceivedAsync("Address OK", executionId);
        }
        else {
            String executionId = getExecutionId("Fake", processInstanceId);
            runtimeService.messageEventReceivedAsync("Fake address", executionId);
        }

    }

    private String getExecutionId(String messageName, String processInstanceId) {
        if (processInstanceId == null) {
            System.out.println("NULL process instance id");
            return null;
        }
        String executionId = null;
        List<EventSubscription> messageSubscriptions = runtimeService.createEventSubscriptionQuery()
                .list();
        for (EventSubscription subscription : messageSubscriptions) {
            if (subscription.getProcessInstanceId().equals(processInstanceId)
                    && subscription.getEventName().equals(messageName)) {
                executionId = subscription.getExecutionId();
                System.out.println("Found: "+ executionId);
                break;
            }
        }
        return executionId;
    }

}