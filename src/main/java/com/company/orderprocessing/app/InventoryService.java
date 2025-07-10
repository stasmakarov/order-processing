package com.company.orderprocessing.app;

import com.company.orderprocessing.record.ItemUpdate;
import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.ItemOperation;
import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component("ord_InventoryService")
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public boolean updateItem(UUID uuid, int quantity, ItemOperation operation) {
        try {
            // Refresh the item to ensure you have the latest version
            Item freshItem = entityManager.find(Item.class, uuid, LockModeType.PESSIMISTIC_WRITE);

            boolean result = true;
            switch (operation) {
                case RESERVATION -> result = reserve(freshItem, quantity);
                case CANCEL_RESERVATION -> cancelReservation(freshItem, quantity);
                case DELIVERY -> deliver(freshItem, quantity);
                case PRODUCTION -> produce(freshItem, quantity);
            }

            if (result) { // if result is true
                entityManager.merge(freshItem);
            }
            return result;
        } catch (Exception e) {
            // Handle specific exceptions here, e.g., OptimisticLockException
            return false;
        }
    }

    private boolean reserve(Item item, int quantity) {
        if (item.getAvailable() < quantity) {
            System.out.println("Not enough stock available for reservation");
            return false;
        }
        item.setAvailable(item.getAvailable() - quantity);
        item.setReserved(item.getReserved() + quantity);
        return true;
    }

    private void cancelReservation(Item item, int quantity) {
        item.setAvailable(item.getAvailable() + quantity);
        item.setReserved(item.getReserved() - quantity);
    }

    private void deliver(Item item, int quantity) {
        item.setDelivered(item.getDelivered() + quantity);
        item.setReserved(item.getReserved() - quantity);
    }

    private void produce(Item item, int quantity) {
        item.setAvailable(item.getAvailable() + quantity);
    }

    public void produceItems(Item item, int producedQty) {
        updateItem(item.getId(), producedQty, ItemOperation.PRODUCTION);
    }

    @Transactional
    public boolean proceedMessage(ItemUpdate itemUpdate) {
        UUID uuid = UUID.fromString(itemUpdate.getId());
        try {
            ItemOperation itemOperation = ItemOperation.fromId(itemUpdate.getOperation());
            if (itemOperation != null) {
                return updateItem(uuid, itemUpdate.getQuantity(), itemOperation);
            } else {
                log.error("Entity not found");
                return false;
            }
        } catch (EntityNotFoundException e) {
            log.error("Entity not found", e);
            return false;
        } catch (OptimisticLockException e) {
            log.error("Optimistic locking occurred", e);
            return false;
        }
    }

}