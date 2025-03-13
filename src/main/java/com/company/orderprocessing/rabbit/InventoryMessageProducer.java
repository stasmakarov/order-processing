package com.company.orderprocessing.rabbit;

import com.company.orderprocessing.record.ItemUpdateRecord;
import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.ItemOperation;
import com.company.orderprocessing.entity.OrderProcessingSettings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.appsettings.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InventoryMessageProducer {
    private static final Logger log = LoggerFactory.getLogger(InventoryMessageProducer.class);
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private AppSettings appSettings;

    public void sendInventoryMessage(Item item, int quantity, ItemOperation operation) {
        String queue = appSettings.load(OrderProcessingSettings.class).getInventoryQueue();
        String json = serialize(item, quantity, operation);
        rabbitTemplate.convertAndSend(queue, json);
        log.info("Inventory JSON: {}", json);
    }


    private String serialize(Item item, int quantity, ItemOperation operation) {
        ItemUpdateRecord updateRecord = new ItemUpdateRecord(item.getId().toString(),
                item.getName(),
                quantity,
                operation.getId()
                );
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(updateRecord);
        } catch (JsonProcessingException e) {
            //noinspection JmixRuntimeException
            throw new RuntimeException(e);
        }
    }
}
