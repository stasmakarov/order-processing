package com.company.orderprocessing.rabbit;

import com.company.orderprocessing.app.InventoryService;
import com.company.orderprocessing.record.ItemUpdate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class InventoryMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(InventoryMessageHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(id = "inventoryMessageListener", queues = "#{@inventoryQueue}", autoStartup = "false")
    public void handleMessage(@Payload String payload,
                              @Header(AmqpHeaders.CORRELATION_ID) String correlationId,
                              @Header(AmqpHeaders.REPLY_TO) String replyTo,
                              Message message,
                              Channel channel) {
        if (payload == null) {
            log.error("Received null payload");
            return;
        }

        try {
            ItemUpdate itemUpdate = mapper.readValue(payload, ItemUpdate.class);
            boolean result = inventoryService.proceedMessage(itemUpdate);

            if (replyTo == null || correlationId == null) {
                throw new IllegalArgumentException("Missing reply metadata");
            }

            sendSuccessReply(replyTo, correlationId, result);
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize payload", e);
            sendErrorReply(replyTo, correlationId);
            try {
                // Reject the message on error and requeue
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            } catch (IOException ex) {
                log.error("Failed to nack the message", ex);
            }
        } catch (Exception e) {
            log.error("Inventory processing failed: {}", e.getMessage(), e);
            sendErrorReply(replyTo, correlationId);
            try {
                // Reject the message on error and requeue
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            } catch (IOException ex) {
                log.error("Failed to nack the message", ex);
            }
        }
    }

    private void sendSuccessReply(String replyTo, String correlationId, boolean result) {
        rabbitTemplate.convertAndSend(
                replyTo,
                result,
                replyMessage -> {
                    replyMessage.getMessageProperties()
                            .setCorrelationId(correlationId);
                    return replyMessage;
                }
        );
    }

    private void sendErrorReply(String replyTo, String correlationId) {
        rabbitTemplate.convertAndSend(
                replyTo,
                false,
                errorMessage -> {
                    errorMessage.getMessageProperties()
                            .setCorrelationId(correlationId);
                    return errorMessage;
                }
        );
    }
}