package com.company.orderprocessing.event;

import org.springframework.context.ApplicationEvent;

public class DeliveryCompletedEvent extends ApplicationEvent {
    private final String deliveryNumber;
    public DeliveryCompletedEvent(Object source, String deliveryNumber) {
        super(source);
        this.deliveryNumber = deliveryNumber;
    }

    public String getDeliveryNumber() {
        return deliveryNumber;
    }
}
