package com.company.orderprocessing.rabbit;

import com.company.orderprocessing.app.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

@Component
public class OrderMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageHandler.class);

    @Autowired
    private OrderService orderService;

    @RabbitListener(id = "orderMessageListener", queues = "#{@orderQueue}", autoStartup = "false")
    public void handleMessage(Message message) {
            try {
                String json = new String(message.getBody());
                log.info("Received order: {}", json);
                orderService.startOrderProcess(json);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }
}
