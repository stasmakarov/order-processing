package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.Item;
import com.vaadin.flow.component.notification.Notification;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.Notifications;
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
    private Notifications notifications;

    public void supplyItems() {
        Random random = new Random();
        systemAuthenticator.begin();
        try {
            SaveContext saveContext = new SaveContext();
            List<Item> items = dataManager.load(Item.class).all().list();
            for (Item item : items) {
                Integer totalQuantity = item.getTotalQuantity();
                item.setTotalQuantity(totalQuantity + random.nextInt(10));
                saveContext.saving(item);
            }
            dataManager.save(saveContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            systemAuthenticator.end();
        }
        notifications.create("Произведена новая партия товаров")
                .withPosition(Notification.Position.BOTTOM_CENTER)
                .withDuration(3000)
                .show();
    }
}