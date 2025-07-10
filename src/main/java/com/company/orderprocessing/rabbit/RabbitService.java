package com.company.orderprocessing.rabbit;

import com.company.orderprocessing.entity.OrderProcessingSettings;
import io.jmix.appsettings.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.*;
import org.springframework.stereotype.Component;

@Component
public class RabbitService {
    private static final Logger log = LoggerFactory.getLogger(RabbitService.class);

    private final RabbitListenerEndpointRegistry endpointRegistry;
    private final ConnectionFactory connectionFactory;
    private final RabbitTemplate rabbitTemplate;
    private final AppSettings appSettings;

    public RabbitService(ConnectionFactory connectionFactory,
                         RabbitListenerEndpointRegistry endpointRegistry,
                         RabbitTemplate rabbitTemplate,
                         AppSettings appSettings) {
        this.connectionFactory = connectionFactory;
        this.endpointRegistry = endpointRegistry;
        this.rabbitTemplate = rabbitTemplate;
        this.appSettings = appSettings;
    }

    public boolean isRabbitAvailable() {
        try {
            RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
            String queueName = appSettings.load(OrderProcessingSettings.class).getOrderQueue();
            rabbitAdmin.getQueueProperties(queueName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean startListening() {
        boolean orderStarted = startContainer("orderMessageListener");
        boolean inventoryStarted = startContainer("inventoryMessageListener");
        boolean replyStarted = startContainer("replyMessageListener");

        return orderStarted && inventoryStarted && replyStarted;
    }


    public boolean startContainer(String containerName) {
        MessageListenerContainer container = endpointRegistry.getListenerContainer(containerName);

        if (container == null) {
            log.error("❌ RabbitMQ listener {} is NULL.", containerName);
            return false;
        }

        if (container.isRunning()) {
            log.warn("⚠️ RabbitMQ {} is already running.", container);
            return false;
        }

        try {
            container.start();
            if (container.isRunning()) {
                log.info("✅ RabbitMQ listener {} started successfully.", containerName);
                return true;
            } else {
                log.error("❌ RabbitMQ listener {} failed to start.", containerName);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Error while starting RabbitMQ listener {}: {}", containerName, e.getMessage(), e);
            return false;
        }
    }


    public void stopListening() {
        stopContainer("orderMessageListener");
        stopContainer("inventoryMessageListener");
        stopContainer("replyMessageListener");
    }

    public void stopContainer(String containerName) {
        MessageListenerContainer container = endpointRegistry.getListenerContainer(containerName);
        if (container != null && container.isRunning()) {
                container.stop();
                log.info("Stopped listening to the queues.");
            }
    }


    public boolean isRabbitRunning() {
        MessageListenerContainer orderListener = endpointRegistry.getListenerContainer("orderMessageListener");
        MessageListenerContainer inventoryListener = endpointRegistry.getListenerContainer("inventoryMessageListener");

        return (orderListener != null) && orderListener.isRunning()
                && (inventoryListener != null) && inventoryListener.isRunning();
    }

    public void purgeQueue(String queueName) {
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(queueName);
            return null;
        });
    }

}
