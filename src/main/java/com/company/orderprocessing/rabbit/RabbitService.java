package com.company.orderprocessing.rabbit;

import com.company.orderprocessing.entity.OrderProcessingSettings;
import io.jmix.appsettings.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.*;
import org.springframework.stereotype.Component;

@Component
public class RabbitService {
    private static final Logger log = LoggerFactory.getLogger(RabbitService.class);

    private final RabbitListenerEndpointRegistry endpointRegistry;
    private final ConnectionFactory connectionFactory;
    private final AppSettings appSettings;

    public RabbitService(ConnectionFactory connectionFactory,
                         RabbitListenerEndpointRegistry endpointRegistry,
                         AppSettings appSettings) {
        this.connectionFactory = connectionFactory;
        this.endpointRegistry = endpointRegistry;
        this.appSettings = appSettings;
    }

    public boolean isRabbitAvailable() {
        try {
            RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
            String queueName = appSettings.load(OrderProcessingSettings.class).getOrderQueue();
            rabbitAdmin.getQueueProperties(queueName); // Attempt to access queue properties
            return true; // If successful, RabbitMQ is available
        } catch (Exception e) {
            return false; // If any exception occurs, RabbitMQ is not available
        }
    }

    public boolean startListening() {
        MessageListenerContainer container = endpointRegistry.getListenerContainer("orderMessageListener");

        if (container == null) {
            log.error("❌ RabbitMQ listener container is NULL");
            return false;
        }

        if (container.isRunning()) {
            log.warn("⚠️ RabbitMQ listener is already running.");
            return false;
        }

        try {
            container.start();
            if (container.isRunning()) {
                log.info("✅ RabbitMQ listener started successfully.");
                return true;
            } else {
                log.error("❌ RabbitMQ listener failed to start.");
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Error while starting RabbitMQ listener: {}", e.getMessage(), e);
            return false;
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
