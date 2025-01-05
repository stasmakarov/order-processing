package com.company.orderprocessing.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static java.time.LocalTime.now;

@Component("ord_RequestEventPublisher")
public class RequestEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RequestEventPublisher.class);
    private final ApplicationEventPublisher applicationEventPublisher;

    public RequestEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void sendAddressVerificationRequest(String address, String processInstanceId) {
        log.info("Event published: initiateAddressVerificationRequest");
        applicationEventPublisher.publishEvent(new RequestSendEvent(this, address, processInstanceId));
    }

    public void notifyAddressVerificationCompletedSuccess(String address, String token) {
        log.info("Event published: notifyAddressVerificationCompleted");
        applicationEventPublisher.publishEvent(new ResponseOkEvent(this, address, token));
    }

    public void notifyAddressVerificationCompletedFailure(String address, String token) {
        log.info("Event published: notifyAddressVerificationCompleted");
        applicationEventPublisher.publishEvent(new ResponseFailEvent(this, address, token));
    }
}