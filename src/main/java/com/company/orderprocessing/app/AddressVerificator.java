package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.event.RequestSendEvent;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component(value = "ord_AddressVerificator")
public class AddressVerificator {

    private static final Logger log = LoggerFactory.getLogger(AddressVerificator.class);

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public void verify(Order order, DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        if (order.getAddress() != null) {
            applicationEventPublisher.publishEvent(new RequestSendEvent(this, order, processInstanceId));
        } else {
            log.error("Order # {}: Address is NULL", order.getNumber());
        }
    }

}