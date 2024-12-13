package com.company.orderprocessing.event;

import org.springframework.context.ApplicationEvent;

public class ResponseFailEvent extends ApplicationEvent {
    private final String address;
    private final String token;
    public ResponseFailEvent(Object source, String address, String token) {
        super(source);
        this.address = address;
        this.token = token;
    }

    public String getAddress() {
        return address;
    }

    public String getToken() {
        return token;
    }
}
