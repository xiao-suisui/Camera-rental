package com.example.camerarental.order.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitProducerCallbackConfig {

    private final RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void initCallbacks() {
        rabbitTemplate.setConfirmCallback((correlation, ack, cause) -> {
            if (!ack) {
                String eventId = correlation == null ? "unknown" : correlation.getId();
                log.error("RabbitMQ publish not acknowledged: eventId={}, cause={}", eventId, cause);
            }
        });

        rabbitTemplate.setReturnsCallback(returned -> log.error(
                "RabbitMQ returned message: exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText()
        ));
    }
}

