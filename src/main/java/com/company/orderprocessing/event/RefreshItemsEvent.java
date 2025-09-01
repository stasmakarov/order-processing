package com.company.orderprocessing.event;

import org.springframework.context.ApplicationEvent;

public class RefreshItemsEvent extends ApplicationEvent {
    public RefreshItemsEvent(Object source) {
        super(source);
    }
}
