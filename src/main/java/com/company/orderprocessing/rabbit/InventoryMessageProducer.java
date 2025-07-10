package com.company.orderprocessing.rabbit;

import com.company.orderprocessing.record.ItemUpdate;
import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.entity.ItemOperation;
import com.company.orderprocessing.entity.OrderProcessingSettings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.appsettings.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InventoryMessageProducer {
    private static final Logger log = LoggerFactory.getLogger(InventoryMessageProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private FutureManager futureManager;
    @Autowired
    private AppSettings appSettings;

    public CompletableFuture<Boolean> sendInventoryMessage(Item item, int quantity, ItemOperation operation) {
        if (item == null || operation == null) {
            throw new NullPointerException("Item or operation cannot be null");
        }

        String inventoryQueue = appSettings.load(OrderProcessingSettings.class).getInventoryQueue();
        String replyQueue = appSettings.load(OrderProcessingSettings.class).getReplyQueue();
        String correlationId = UUID.randomUUID().toString();
        ItemUpdate itemUpdate = new ItemUpdate(item.getId().toString(), item.getName(), quantity, operation.getId());

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        futureManager.addFuture(correlationId, future);

        MessagePostProcessor messagePostProcessor = message -> {
            message.getMessageProperties().setCorrelationId(correlationId);
            message.getMessageProperties().setReplyTo(replyQueue);
            message.getMessageProperties().setContentType("application/json");
            return message;
        };

        try {
            rabbitTemplate.convertAndSend(inventoryQueue, itemUpdate, messagePostProcessor);
            log.info("Sent message to inventory queue with correlationId {}", correlationId);
        } catch (AmqpException e) {
            log.error("Error sending message", e);
            future.completeExceptionally(e);
        }
        return future;
    }
}

