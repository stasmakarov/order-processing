package com.company.orderprocessing.event;

import org.springframework.context.ApplicationEvent;

public class NewItemsProducedEvent extends ApplicationEvent {
    public NewItemsProducedEvent(Object source) {
        super(source);
    }
}
