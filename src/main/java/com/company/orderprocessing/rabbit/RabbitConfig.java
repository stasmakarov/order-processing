package com.company.orderprocessing.rabbit;

import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitConfig implements RabbitListenerConfigurer {

    @Bean
    public Queue readQueue() {
        return new Queue("orders");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        // Create a SimpleRabbitListenerEndpoint
        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId("orderMessageListener");
        endpoint.setQueueNames("orders"); // Specify the queue name
        endpoint.setMessageListener(new MessageListenerAdapter(orderMessageListener())); // Set the message listener
        endpoint.setAutoStartup(false);
        // Register the endpoint with the registrar
        registrar.registerEndpoint(endpoint);
    }

    @Bean
    public OrderMessageListener orderMessageListener() {
        return new OrderMessageListener();
    }
}
