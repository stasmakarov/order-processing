package com.company.orderprocessing.app;

import com.company.orderprocessing.record.ItemUpdateRecord;
import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.ItemOperation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component("ord_InventoryService")
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    @Autowired
    private DataManager dataManager;
    @Autowired
    private SystemAuthenticator systemAuthenticator;

    @Transactional
    public boolean update(Item item, int quantity, ItemOperation operation) {
        systemAuthenticator.begin("admin");
        try {
            switch (operation) {
                case RESERVATION -> {
                    if (item.getAvailable() < quantity) {
                        return false;
                    } else {
                        item.setAvailable(item.getAvailable() - quantity);
                        item.setReserved(item.getReserved() + quantity);
                        dataManager.save(item);
                        return true;
                    }
                }
                case CANCEL_RESERVATION -> {
                        item.setAvailable(item.getAvailable() + quantity);
                        item.setReserved(item.getReserved() - quantity);
                        dataManager.save(item);
                        return true;
                }
                case DELIVERY -> {
                        item.setDelivered(item.getDelivered() + quantity);
                        item.setReserved(item.getReserved() - quantity);
                        dataManager.save(item);
                        return true;
                }
                case PRODUCTION -> {
                    item.setAvailable(item.getAvailable() + quantity);
                    dataManager.save(item);
                }
            }
        } finally {
            systemAuthenticator.end();
        }
        return false;
    }

    public void produceItems(Item item, int producedQty) {
//        update(item, producedQty, ItemOperation.PRODUCTION);
    }

    public void proceedMessage(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ItemUpdateRecord itemUpdateDto = objectMapper.readValue(json, ItemUpdateRecord.class);
            UUID uuid = UUID.fromString(itemUpdateDto.id());
            Item item = dataManager.load(Item.class).id(uuid).optional().orElse(null);
            ItemOperation itemOperation = ItemOperation.fromId(itemUpdateDto.operation());
            if (itemOperation != null) {
                update(item, itemUpdateDto.quantity(), itemOperation);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}