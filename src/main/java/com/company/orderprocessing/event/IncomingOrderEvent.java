package com.company.orderprocessing.event;

import com.company.orderprocessing.entity.Order;
import org.springframework.context.ApplicationEvent;

public class IncomingOrderEvent extends ApplicationEvent {
    private final String order;
    public IncomingOrderEvent(Object source, String order) {
        super(source);
        this.order = order;
    }

    public String getOrder() {
        return order;
    }
}
