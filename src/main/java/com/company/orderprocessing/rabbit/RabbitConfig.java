package com.company.orderprocessing.rabbit;

import com.company.orderprocessing.entity.OrderProcessingSettings;
import io.jmix.appsettings.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableRabbit
public class RabbitConfig {

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost");
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        return connectionFactory;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public String orderQueue(AppSettings appSettings) {
        return appSettings.load(OrderProcessingSettings.class).getOrderQueue();
    }

    @Bean
    public String inventoryQueue(AppSettings appSettings) {
        return appSettings.load(OrderProcessingSettings.class).getInventoryQueue();
    }

    @Bean
    public String replyQueue(AppSettings appSettings) {
        return appSettings.load(OrderProcessingSettings.class).getReplyQueue();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public SimpleMessageListenerContainer orderListenerContainer(ConnectionFactory connectionFactory,
                    RabbitMQConnectionService connectionService,
                    @Qualifier("orderListenerAdapter") MessageListenerAdapter listenerAdapter,
                    AppSettings appSettings) {
        String queueName = appSettings.load(OrderProcessingSettings.class).getOrderQueue();
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionService.getConnectionFactory());
        container.setQueueNames(queueName);
        container.setMessageListener(listenerAdapter);
        container.setAutoStartup(false);
        container.stop();
        return container;
    }

    @Bean
    @Qualifier("orderListenerAdapter")
    public MessageListenerAdapter orderListenerAdapter(OrderMessageHandler handler) {
        return new MessageListenerAdapter(handler, "handleMessage");
    }

    @Bean
    public SimpleMessageListenerContainer inventoryListenerContainer(ConnectionFactory connectionFactory,
                    RabbitMQConnectionService connectionService,
                    @Qualifier("inventoryListenerAdapter") MessageListenerAdapter listenerAdapter,
                    AppSettings appSettings) {
        String queueName = appSettings.load(OrderProcessingSettings.class).getInventoryQueue();
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionService.getConnectionFactory());
        container.setQueueNames(queueName);
        container.setMessageListener(listenerAdapter);
        container.setAcknowledgeMode(AcknowledgeMode.NONE);
        container.setAutoStartup(false);
        container.stop();
        return container;
    }

    @Bean
    @Qualifier("inventoryListenerAdapter")
    public MessageListenerAdapter inventoryMessageAdapter(InventoryMessageHandler handler) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(handler, "handleMessage");
        adapter.setMessageConverter(messageConverter());
        return adapter;
    }


    @Bean
    public SimpleMessageListenerContainer replyListenerContainer(ConnectionFactory connectionFactory,
                    RabbitMQConnectionService connectionService,
                    @Qualifier("replyListenerAdapter") MessageListenerAdapter listenerAdapter,
                    AppSettings appSettings) {
        String replyQueueName = appSettings.load(OrderProcessingSettings.class).getReplyQueue();
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionService.getConnectionFactory());
        container.setQueueNames(replyQueueName);
        container.setMessageListener(listenerAdapter);
        container.setAutoStartup(false);
        container.stop();
        return container;
    }

    @Bean
    @Qualifier("replyListenerAdapter")
    public MessageListenerAdapter replyListenerAdapter(ReplyMessageHandler handler) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(handler, "handleReply");
        adapter.setMessageConverter(messageConverter());
        return adapter;
    }
}
