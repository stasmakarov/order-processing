package com.company.orderprocessing.event;

import org.springframework.context.ApplicationEvent;

public class RefreshViewEvent extends ApplicationEvent {
    public RefreshViewEvent(Object source) {
        super(source);
    }
}
