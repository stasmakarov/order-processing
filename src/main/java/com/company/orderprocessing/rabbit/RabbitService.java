package com.company.orderprocessing.rabbit;

import com.company.orderprocessing.entity.OrderProcessingSettings;
import io.jmix.appsettings.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RabbitService {
    private static final Logger log = LoggerFactory.getLogger(RabbitService.class);
    private boolean isRegistered = false;

    @Autowired
    private ConnectionFactory connectionFactory;
    @Autowired
    private RabbitListenerEndpointRegistry endpointRegistry;
    @Autowired
    private AppSettings appSettings;

    public boolean isRabbitMqAvailable() {
        try {
            connectionFactory.createConnection().close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRabbitAvailable() {
        try {
            RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
            String queueName = appSettings.load(OrderProcessingSettings.class).getQueueName();
            rabbitAdmin.getQueueProperties(queueName); // Attempt to access queue properties
            return true; // If successful, RabbitMQ is available
        } catch (Exception e) {
            return false; // If any exception occurs, RabbitMQ is not available
        }
    }

    public void startListening() {
        MessageListenerContainer container = endpointRegistry.getListenerContainer("orderMessageListener");
        if (container != null && !container.isRunning()) {
            container.start();
            log.info("Started listening to the queue.");
        }
    }


    public void stopListening() {
        MessageListenerContainer container = endpointRegistry.getListenerContainer("orderMessageListener");
        if (container != null && container.isRunning()) {
                container.stop();
                log.info("Stopped listening to the queue.");
            }
    }


    public boolean isRabbitRunning() {
        MessageListenerContainer listener = endpointRegistry.getListenerContainer("orderMessageListener");
        return (listener != null) && listener.isRunning();
    }

}
