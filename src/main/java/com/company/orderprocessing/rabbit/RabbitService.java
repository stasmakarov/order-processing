package com.company.orderprocessing.rabbit;

import com.company.orderprocessing.entity.OrderProcessingSettings;
import io.jmix.appsettings.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Properties;

/**
 * RabbitService
 */
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
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
        this.endpointRegistry = Objects.requireNonNull(endpointRegistry);
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate);
        this.appSettings = Objects.requireNonNull(appSettings);
    }

    // ---------------------------------------------------------------------
    // RabbitAdmin helper
    // ---------------------------------------------------------------------

    /**
     * Build RabbitAdmin on demand. No AmqpAdmin bean required.
     */
    private RabbitAdmin admin() {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    // ---------------------------------------------------------------------
    // Queue declaration
    // ---------------------------------------------------------------------

    /** Ensure queues from settings exist (declare if missing). */
    public void ensureQueuesExist() {
        OrderProcessingSettings s = appSettings.load(OrderProcessingSettings.class);
        declareDurableIfMissing(s.getOrderQueue());
        declareDurableIfMissing(s.getInventoryQueue());
        declareDurableIfMissing(s.getReplyQueue());
    }

    private void declareDurableIfMissing(String queueName) {
        if (queueName == null || queueName.isBlank()) return;
        Properties props = admin().getQueueProperties(queueName); // spring-rabbit 3.1.x returns Properties
        if (props == null) {
            // durable=true, exclusive=false, autoDelete=false
            log.info("Declaring missing queue '{}' (durable)", queueName);
            admin().declareQueue(new Queue(queueName, true, false, false));
        }
    }

    // ---------------------------------------------------------------------
    // Availability
    // ---------------------------------------------------------------------

    public boolean isRabbitAvailable() {
        try {
            String queueName = appSettings.load(OrderProcessingSettings.class).getOrderQueue();
            return admin().getQueueProperties(queueName) != null;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------------------------------------------------------------
    // Listeners lifecycle
    // ---------------------------------------------------------------------

    public boolean startListening() {
        ensureQueuesExist(); // avoid "Failed to declare queue" on startup
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
            log.warn("⚠️ RabbitMQ listener {} is already running.", containerName);
            return true;
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
            log.info("Stopped listener {}.", containerName);
        }
    }

    public boolean isRabbitRunning() {
        MessageListenerContainer orderListener = endpointRegistry.getListenerContainer("orderMessageListener");
        MessageListenerContainer inventoryListener = endpointRegistry.getListenerContainer("inventoryMessageListener");
        return (orderListener != null) && orderListener.isRunning()
                && (inventoryListener != null) && inventoryListener.isRunning();
    }

    // ---------------------------------------------------------------------
    // Purge (idempotent)
    // ---------------------------------------------------------------------

    /** Idempotent purge: skip when queue is missing in current vhost. */
    public void purgeQueue(String queueName) {
        if (queueName == null || queueName.isBlank()) {
            log.warn("purgeQueue called with empty name, skipping");
            return;
        }
        Properties props = admin().getQueueProperties(queueName);
        if (props == null) {
            log.info("Queue '{}' not found, skipping purge", queueName);
            return;
        }
        admin().purgeQueue(queueName, false);
        log.info("Queue '{}' purged", queueName);
    }

    /** Soft purge wrapper to avoid breaking UI actions. */
    public void safePurge(String name) {
        try { purgeQueue(name); }
        catch (Exception e) { log.warn("Purge failed for '{}': {}", name, e.toString()); }
    }
}
