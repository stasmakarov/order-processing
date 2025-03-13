package com.company.orderprocessing.event;

import com.company.orderprocessing.entity.Item;
import org.springframework.context.ApplicationEvent;

public class ItemsSuspendedEvent extends ApplicationEvent {
    private final Item item;
    public ItemsSuspendedEvent(Object source, Item item) {
        super(source);
        this.item = item;
    }

    public Item getItem() {
        return item;
    }
}
