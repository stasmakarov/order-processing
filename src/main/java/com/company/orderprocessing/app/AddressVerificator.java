package com.company.orderprocessing.app;

import com.company.orderprocessing.publisher.RequestEventPublisher;
import com.company.orderprocessing.nominatim.GeoCodingService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "ord_AddressVerificator")
public class AddressVerificator {

    private static final Logger log = LoggerFactory.getLogger(AddressVerificator.class);

    @Autowired
    private GeoCodingService geoCodingService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RequestEventPublisher requestEventPublisher;

    public void verify(String address, DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        requestEventPublisher.sendAddressVerificationRequest(address, processInstanceId);
    }

    // This method for debugging process
//    public void verifySimple(String address, String processInstanceId) {
//        Point point = geoCodingService.verifyAddress(address);
//        boolean addressVerificationResult = (point != null);
//        if (addressVerificationResult) {
//            String executionId = getExecutionId("OK", processInstanceId);
//            runtimeService.messageEventReceivedAsync("Address OK", executionId);
//        }
//        else {
//            String executionId = getExecutionId("Fake", processInstanceId);
//            runtimeService.messageEventReceivedAsync("Fake address", executionId);
//        }
//
//    }
//
//    private String getExecutionId(String messageName, String processInstanceId) {
//        if (processInstanceId == null) {
//            log.error("NULL process instance id");
//            return null;
//        }
//        String executionId = null;
//        List<EventSubscription> messageSubscriptions = runtimeService.createEventSubscriptionQuery()
//                .list();
//        for (EventSubscription subscription : messageSubscriptions) {
//            if (subscription.getProcessInstanceId().equals(processInstanceId)
//                    && subscription.getEventName().equals(messageName)) {
//                executionId = subscription.getExecutionId();
//                break;
//            }
//        }
//        return executionId;
//    }

}