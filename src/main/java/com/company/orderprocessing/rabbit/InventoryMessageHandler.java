package com.company.orderprocessing.rabbit;

import com.company.orderprocessing.app.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;

public class InventoryMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(InventoryMessageHandler.class);

    @Autowired
    private InventoryService inventoryService;

    @RabbitListener
    public void handleMessage(Message message) {
        try {
            String json = new String(message.getBody());
            log.info("Received inventory message: {}", json);
            inventoryService.proceedMessage(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
