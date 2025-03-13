package com.company.orderprocessing.app;

import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.OrderProcessingSettings;
import com.company.orderprocessing.event.ItemsProducedEvent;
import io.jmix.appsettings.AppSettings;
import io.jmix.flowui.UiEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component(value = "ord_ManufacturingService")
public class ManufacturingService {

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private UiEventPublisher uiEventPublisher;
    @Autowired
    private AppSettings appSettings;

    private final Random random = new Random();

    public void produceItems(Item item) {
        Integer maxItemsParty = appSettings.load(OrderProcessingSettings.class).getMaxItemsParty();
        int producedQty = random.nextInt(maxItemsParty);
        inventoryService.produceItems(item, producedQty);
        uiEventPublisher.publishEvent(new ItemsProducedEvent(this, item));
    }
}