package com.company.orderprocessing.rabbit;

import com.company.orderprocessing.app.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderMessageListener {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageListener.class);

    @Autowired
    private OrderService orderService;

    @RabbitListener(queues = "orders", containerFactory = "rabbitListenerContainerFactory")
    public void handleMessage(String json) {
        log.info("Received order: {}", json);
        orderService.startOrderProcess(json);
    }
}
