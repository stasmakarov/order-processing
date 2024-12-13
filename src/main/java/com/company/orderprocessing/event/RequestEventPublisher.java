package com.company.orderprocessing.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static java.time.LocalTime.now;

@Component("ord_RequestEventPublisher")
public class RequestEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public RequestEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void initiateAddressVerificationRequest(String address, String processInstanceId) {
        System.out.println("Event published: initiateAddressVerificationRequest");
        applicationEventPublisher.publishEvent(new RequestSendEvent(this, address, processInstanceId));
    }

    public void notifyAddressVerificationCompletedSuccess(String address, String token) {
        System.out.println("Event published: notifyAddressVerificationCompleted");
        applicationEventPublisher.publishEvent(new ResponseOkEvent(this, address, token));
    }

    public void notifyAddressVerificationCompletedFailure(String address, String token) {
        System.out.println("Event published: notifyAddressVerificationCompleted");
        applicationEventPublisher.publishEvent(new ResponseFailEvent(this, address, token));
    }
}