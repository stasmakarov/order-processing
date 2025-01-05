package com.company.orderprocessing.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RabbitService {
    private static final Logger log = LoggerFactory.getLogger(RabbitService.class);

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    public boolean isRabbitAvailable() {
        try {
            RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
            rabbitAdmin.getQueueProperties("orders"); // Attempt to access queue properties
            return true; // If successful, RabbitMQ is available
        } catch (Exception e) {
            return false; // If any exception occurs, RabbitMQ is not available
        }
    }

    public void startListening() {
        rabbitListenerEndpointRegistry.getListenerContainers().forEach(container -> {
            if (!container.isRunning()) {
                container.start(); // Start each listener container
                log.info("Started listening to the queue.");
            } else {
                log.info("RabbitMQ is not available. Cannot start listening.");
            }
        });
    }


    public void stopListening() {
        rabbitListenerEndpointRegistry.getListenerContainers().forEach(container -> {
            if (container.isRunning()) {
                container.stop(); // Stop each listener container
                log.info("Stopped listening to the queue.");
            }
        });
    }


    public boolean isRabbitRunning() {
        MessageListenerContainer listener = rabbitListenerEndpointRegistry.getListenerContainer("orderMessageListener");
        return listener.isRunning();
    }
}
