package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.OrderProcessingSettings;
import com.company.orderprocessing.event.NewItemsProducedEvent;
import com.vaadin.flow.component.notification.Notification;
import io.jmix.appsettings.AppSettings;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component(value = "ord_ManufacturingService")
public class ManufacturingService {

    @Autowired
    private SystemAuthenticator systemAuthenticator;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private UiEventPublisher uiEventPublisher;
    @Autowired
    private AppSettings appSettings;

    private final Random random = new Random();

    public void supplyItems() {
        Integer maxItemsParty = appSettings.load(OrderProcessingSettings.class).getMaxItemsParty();
        systemAuthenticator.begin("admin");
        try {
            SaveContext saveContext = new SaveContext();
            List<Item> items = dataManager.load(Item.class).all().list();
            for (Item item : items) {
                Integer availableQty = item.getAvailable();
                item.setAvailable(availableQty + random.nextInt(maxItemsParty));
                saveContext.saving(item);
            }
            dataManager.save(saveContext);
            uiEventPublisher.publishEvent(new NewItemsProducedEvent(this));
        } catch (Exception e) {
            //noinspection JmixRuntimeException
            throw new RuntimeException(e);
        } finally {
            systemAuthenticator.end();
        }
    }
}