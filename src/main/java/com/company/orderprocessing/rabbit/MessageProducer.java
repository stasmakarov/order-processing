//package com.company.orderprocessing.rabbit;
//
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//@Component
//public class MessageProducer {
//    @Autowired
//    private RabbitTemplate rabbitTemplate;
//
//    public void sendMessage(String message) {
//        rabbitTemplate.convertAndSend("orders", message);
//    }
//}
