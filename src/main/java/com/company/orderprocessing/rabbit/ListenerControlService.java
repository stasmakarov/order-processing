package com.company.orderprocessing.rabbit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ListenerControlService {

    @Autowired
    private RabbitListenerEndpointRegistry registry;

    private static final Logger log = LoggerFactory.getLogger(ListenerControlService.class);

    public void startListener() {
        registry.getListenerContainer("orderListener").start();
        log.info("Order listener started.");
    }

    public void stopListener() {
        registry.getListenerContainer("orderListener").stop();
        log.info("Order listener stopped.");
    }

    public boolean isListenerRunning() {
        var container = registry.getListenerContainer("orderListener");
        if (container != null) {
            return container.isRunning(); // true if running, false if stopped
        }
        throw new IllegalArgumentException("No listener found with ID: " + "orderListener");
    }

}

