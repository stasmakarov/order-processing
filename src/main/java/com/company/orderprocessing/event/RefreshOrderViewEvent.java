package com.company.orderprocessing.event;

import org.springframework.context.ApplicationEvent;

public class RefreshOrderViewEvent extends ApplicationEvent {
    private final String orderNumber;
    public RefreshOrderViewEvent(Object source, String orderNumber) {
        super(source);
        this.orderNumber = orderNumber;
    }

    public String getOrderNumber() {
        return orderNumber;
    }
}
