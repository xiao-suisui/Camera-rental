package com.example.camerarental.user.mq;

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
public class UserRabbitMqConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue userNotifyQueue() {
        return QueueBuilder.durable(MqConstants.USER_NOTIFY_QUEUE)
                .withArgument("x-dead-letter-exchange", MqConstants.ORDER_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MqConstants.USER_NOTIFY_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue userNotifyDlq() {
        return QueueBuilder.durable(MqConstants.USER_NOTIFY_DLQ).build();
    }

    @Bean
    public Binding userNotifyBinding(Queue userNotifyQueue, TopicExchange orderTopicExchange) {
        return BindingBuilder.bind(userNotifyQueue)
                .to(orderTopicExchange)
                .with(MqConstants.ORDER_NOTIFY_USER_ROUTING_KEY);
    }

    @Bean
    public Binding userNotifyDlqBinding(Queue userNotifyDlq, DirectExchange orderDeadLetterExchange) {
        return BindingBuilder.bind(userNotifyDlq)
                .to(orderDeadLetterExchange)
                .with(MqConstants.USER_NOTIFY_DLQ_ROUTING_KEY);
    }
}

