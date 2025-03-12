package com.company.orderprocessing.event;

import com.company.orderprocessing.entity.Item;
import org.springframework.context.ApplicationEvent;

public class ItemsProducedEvent extends ApplicationEvent {
    private final Item item;
    public ItemsProducedEvent(Object source, Item item) {
        super(source);
        this.item = item;
    }

    public Item getItem() {
        return item;
    }
}
