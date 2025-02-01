package com.company.orderprocessing.listener;

import com.company.orderprocessing.entity.Item;
import io.jmix.core.Id;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.event.AttributeChanges;
import io.jmix.core.event.EntityChangedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component("cst_ItemEventListener")
public class ItemEventListener {

    @Autowired
    private UnconstrainedDataManager unconstrainedDataManager;

    @EventListener
    public void onItemChangedBeforeCommit(final EntityChangedEvent<Item> event) {
        Id<Item> entityId = event.getEntityId();
        Item item = unconstrainedDataManager.load(entityId).one();
        Integer reserved = item.getReserved();
        if (reserved < 0) {
            throw new RuntimeException("Reservation error");
        }
    }
}