package com.company.orderprocessing.rabbit;

import com.company.orderprocessing.entity.OrderProcessingSettings;
import io.jmix.appsettings.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableRabbit
public class RabbitConfig {
    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost");
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        return connectionFactory;
    }
    @Bean
    public org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new org.springframework.amqp.rabbit.core.RabbitTemplate(connectionFactory);
    }

    @Bean
    public String orderQueueName(AppSettings appSettings) {
        return appSettings.load(OrderProcessingSettings.class).getOrderQueue();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(3); // Adjust based on load
        factory.setMaxConcurrentConsumers(10);
        factory.setDefaultRequeueRejected(false); // Prevent requeuing on failure
        return factory;
    }

    @Bean
    public SimpleMessageListenerContainer listenerContainer(ConnectionFactory connectionFactory,
                                                            RabbitMQConnectionService connectionService,
                                                            MessageListenerAdapter listenerAdapter,
                                                            AppSettings appSettings) {
        String queueName = appSettings.load(OrderProcessingSettings.class).getOrderQueue();
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionService.getConnectionFactory());
        container.setQueueNames(queueName);
        container.setMessageListener(listenerAdapter);
        container.setAutoStartup(false);
        container.stop(); // Ensure it starts off
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(OrderMessageHandler handler) {
        return new MessageListenerAdapter(handler, "handleMessage");
    }
}
