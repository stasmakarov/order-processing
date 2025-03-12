package com.company.orderprocessing.publisher;

import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.event.RequestSendEvent;
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

    public void sendAddressVerificationRequest(Order order, String processInstanceId) {
        log.info("Event published: initiateAddressVerificationRequest");
        applicationEventPublisher.publishEvent(new RequestSendEvent(this, order, processInstanceId));
    }

}