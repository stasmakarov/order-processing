package com.company.orderprocessing.listener;

import com.company.orderprocessing.app.ManufacturingService;
import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.OrderProcessingSettings;
import com.company.orderprocessing.event.ItemsProducedEvent;
import com.company.orderprocessing.event.ItemsSuspendedEvent;
import com.company.orderprocessing.event.RefreshItemsEvent;
import io.jmix.appsettings.AppSettings;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.event.EntityChangedEvent;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.UiEventPublisher;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component("cst_ItemEventListener")
public class ItemEventListener {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ItemEventListener.class);

    @Autowired
    private DataManager dataManager;
    @Autowired
    private SystemAuthenticator systemAuthenticator;
    @Autowired
    private UiEventPublisher uiEventPublisher;
    @Autowired
    private AppSettings appSettings;
    @Autowired
    private ManufacturingService manufacturingService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onItemChangedBeforeCommit(EntityChangedEvent<Item> event) {
        if (event.getType() != EntityChangedEvent.Type.UPDATED) return;
        if (!event.getChanges().isChanged("available") && !event.getChanges().isChanged("reserved")) return;

        systemAuthenticator.begin("admin");
        try {
            Id<Item> entityId = event.getEntityId();
            Item item = dataManager.load(entityId).one();


            int available = nvl(item.getAvailable());
            int reserved = nvl(item.getReserved());


// Basic invariant check (do not mutate state here)
            if (reserved < 0) {
                log.warn("Item {} has negative reserved={} before commit", item.getId(), reserved);
            }

            OrderProcessingSettings settings = appSettings.load(OrderProcessingSettings.class);
            Integer minItemsAvailable = settings.getMinItemsAvailable();
            Integer initialItemQuantity = settings.getInitialItemQuantity();
            if (minItemsAvailable == null) minItemsAvailable = 0;
            if (initialItemQuantity == null) initialItemQuantity = Integer.MAX_VALUE;


// Use mutually exclusive conditions to avoid double triggers
            if (available < minItemsAvailable) {
                manufacturingService.startOrResumeItemsProduction(item);
            } else if (available > initialItemQuantity) {
                manufacturingService.suspendItemsProduction(item);
            }
        } finally {
            systemAuthenticator.end();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onItemChangedAfterCommit(EntityChangedEvent<Item> event) {
        if (event.getType() != EntityChangedEvent.Type.UPDATED) return;
        if (!event.getChanges().isChanged("available") && !event.getChanges().isChanged("reserved")) return;


        systemAuthenticator.begin("admin");
        try {
            Item item = dataManager.load(event.getEntityId()).one();


            int available = nvl(item.getAvailable());
            OrderProcessingSettings settings = appSettings.load(OrderProcessingSettings.class);
            Integer minItemsAvailable = settings.getMinItemsAvailable();
            Integer initialItemQuantity = settings.getInitialItemQuantity();
            if (minItemsAvailable == null) minItemsAvailable = 0;
            if (initialItemQuantity == null) initialItemQuantity = Integer.MAX_VALUE;


            if (available < minItemsAvailable) {
                uiEventPublisher.publishEvent(new ItemsProducedEvent(this, item));
            } else if (available > initialItemQuantity) {
                uiEventPublisher.publishEvent(new ItemsSuspendedEvent(this, item));
                uiEventPublisher.publishEvent(new RefreshItemsEvent(this));
            }
        } finally {
            systemAuthenticator.end();
        }
    }

    private static int nvl(Integer v) { return v == null ? 0 : v; }

}