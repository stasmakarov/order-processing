package com.company.orderprocessing.event;

import com.company.orderprocessing.entity.Order;
import org.springframework.context.ApplicationEvent;

public class RequestSendEvent extends ApplicationEvent {

    private final Order order;
    private final String processInstanceId;

    public RequestSendEvent(Object source, Order order, String processInstanceId) {
        super(source);
        this.order = order;
        this.processInstanceId = processInstanceId;
    }


    public Order getOrder() {
        return order;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }
}
