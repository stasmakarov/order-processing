package com.company.orderprocessing.event;

import org.springframework.context.ApplicationEvent;

public class ResponseOkEvent extends ApplicationEvent {

    private final String address;
    private final String processInstanceId;
    public ResponseOkEvent(Object source, String address, String processInstanceId) {
        super(source);
        this.address = address;
        this.processInstanceId = processInstanceId;
    }

    public String getAddress() {
        return address;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }
}
