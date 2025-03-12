package com.company.orderprocessing.event;

import org.springframework.context.ApplicationEvent;

public class NewItemsSuppliedEvent extends ApplicationEvent {
    public NewItemsSuppliedEvent(Object source) {
        super(source);
    }
}
