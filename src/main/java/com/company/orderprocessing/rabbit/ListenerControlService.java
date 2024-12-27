package com.company.orderprocessing.rabbit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ListenerControlService {

    private static final Logger log = LoggerFactory.getLogger(ListenerControlService.class);

    @Autowired
    private SimpleMessageListenerContainer messageListenerContainer;

    public void startListening() {
        if (!messageListenerContainer.isRunning()) {
            messageListenerContainer.start();
            log.info("Started listening to the queue.");
        }
    }

    public void stopListening() {
        if (messageListenerContainer.isRunning()) {
            messageListenerContainer.stop();
            log.info("Stopped listening to the queue.");
        }
    }

    public boolean isListenerRunning() {
        return messageListenerContainer.isRunning();
    }


//    @Autowired
//    private RabbitListenerEndpointRegistry registry;
//
//    private static final Logger log = LoggerFactory.getLogger(ListenerControlService.class);
//
//    public void startListener() {
//        registry.getListenerContainer("orderListener").start();
//        log.info("Order listener started.");
//    }
//
//    public void stopListener() {
//        registry.getListenerContainer("orderListener").stop();
//        log.info("Order listener stopped.");
//    }
//
//    public boolean isListenerRunning() {
//        var container = registry.getListenerContainer("orderListener");
//        if (container != null) {
//            return container.isRunning(); // true if running, false if stopped
//        }
//        throw new IllegalArgumentException("No listener found with ID: " + "orderListener");
//    }
//
}

