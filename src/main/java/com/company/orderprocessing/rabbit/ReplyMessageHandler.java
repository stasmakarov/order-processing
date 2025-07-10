package com.company.orderprocessing.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReplyMessageHandler {
    @Autowired
    private FutureManager futureManager;

    @RabbitListener(id = "replyMessageListener", queues = "#{@replyQueue}", autoStartup = "false")
    public void handleReply(@Payload String payload,
                            @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {

        ObjectMapper mapper = new ObjectMapper();
        Boolean response = null;
        try {
            response = mapper.readValue(payload, Boolean.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        CompletableFuture<Boolean> future = futureManager.getFuture(correlationId);
        if (future != null) {
            future.complete(response);
            futureManager.removeFuture(correlationId);
        }
    }
}
