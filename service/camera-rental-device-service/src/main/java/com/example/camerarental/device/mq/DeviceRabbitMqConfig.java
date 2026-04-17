package com.example.camerarental.device.mq;

import com.example.camerarental.common.mq.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeviceRabbitMqConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange orderTopicExchange() {
        return new TopicExchange(MqConstants.ORDER_TOPIC_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange orderDeadLetterExchange() {
        return new DirectExchange(MqConstants.ORDER_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue deviceSyncQueue() {
        return QueueBuilder.durable(MqConstants.DEVICE_SYNC_QUEUE)
                .withArgument("x-dead-letter-exchange", MqConstants.ORDER_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MqConstants.DEVICE_SYNC_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue deviceSyncDlq() {
        return QueueBuilder.durable(MqConstants.DEVICE_SYNC_DLQ).build();
    }

    @Bean
    public Binding deviceSyncBinding() {
        return BindingBuilder.bind(deviceSyncQueue())
                .to(orderTopicExchange())
                .with(MqConstants.ORDER_CANCELLED_RELEASE_STOCK_KEY);
    }

    @Bean
    public Binding deviceSyncDlqBinding() {
        return BindingBuilder.bind(deviceSyncDlq())
                .to(orderDeadLetterExchange())
                .with(MqConstants.DEVICE_SYNC_DLQ_ROUTING_KEY);
    }
}

