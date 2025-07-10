package com.company.orderprocessing.listener;

import com.company.orderprocessing.app.ManufacturingService;
import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.OrderProcessingSettings;
import com.company.orderprocessing.event.ItemsProducedEvent;
import com.company.orderprocessing.event.ItemsSuspendedEvent;
import com.company.orderprocessing.util.Iso8601Converter;
import io.jmix.appsettings.AppSettings;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.event.EntityChangedEvent;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.UiEventPublisher;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component("cst_ItemEventListener")
public class ItemEventListener {

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
    @TransactionalEventListener
    public void onItemChangedBeforeCommit(final EntityChangedEvent<Item> event) {
        OrderProcessingSettings settings = appSettings.load(OrderProcessingSettings.class);
        Integer minItemsAvailable = settings.getMinItemsAvailable();
        Integer initialItemQuantity = settings.getInitialItemQuantity();

        systemAuthenticator.begin("admin");
        try {
            Id<Item> entityId = event.getEntityId();
            Item item = dataManager.load(entityId).one();

            if (item.getAvailable() < minItemsAvailable) {
                manufacturingService.startOrResumeItemsProduction(item);
                uiEventPublisher.publishEvent(new ItemsProducedEvent(this, item));
            }

            if (item.getAvailable() > initialItemQuantity) {
                manufacturingService.suspendItemsProduction(item);
                uiEventPublisher.publishEvent(new ItemsSuspendedEvent(this, item));
            }

            Integer reserved = item.getReserved();
            if (reserved < 0) {
                System.out.println("reservation error");
            }
        } finally {
            systemAuthenticator.end();
        }
    }

}