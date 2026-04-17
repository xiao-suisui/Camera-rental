package com.example.camerarental.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.camerarental.common.mq.MqConstants;
import com.example.camerarental.common.mq.OrderCancelledEvent;
import com.example.camerarental.common.mq.OrderNotificationEvent;
import com.example.camerarental.order.entity.MqOutboxMessage;
import com.example.camerarental.order.mapper.MqOutboxMessageMapper;
import com.example.camerarental.order.mq.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublishJob {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY = 10;

    private final MqOutboxMessageMapper outboxMessageMapper;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 5000)
    public void publishOutboxMessages() {
        LocalDateTime now = LocalDateTime.now();
        List<MqOutboxMessage> messages = outboxMessageMapper.selectList(
                new LambdaQueryWrapper<MqOutboxMessage>()
                        .and(w -> w.eq(MqOutboxMessage::getStatus, OutboxStatus.NEW)
                                .or()
                                .eq(MqOutboxMessage::getStatus, OutboxStatus.FAILED)
                                .le(MqOutboxMessage::getNextRetryAt, now))
                        .orderByAsc(MqOutboxMessage::getId)
                        .last("limit " + BATCH_SIZE)
        );

        for (MqOutboxMessage message : messages) {
            publishOne(message);
        }
    }

    private void publishOne(MqOutboxMessage message) {
        try {
            Object event = toEventObject(message);
            rabbitTemplate.convertAndSend(
                    MqConstants.ORDER_TOPIC_EXCHANGE,
                    message.getRoutingKey(),
                    event,
                    new CorrelationData(message.getEventId())
            );

            LocalDateTime now = LocalDateTime.now();
            message.setStatus(OutboxStatus.SENT);
            message.setSentAt(now);
            message.setUpdatedAt(now);
            outboxMessageMapper.updateById(message);
        } catch (Exception ex) {
            int retryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();
            retryCount++;

            LocalDateTime now = LocalDateTime.now();
            message.setRetryCount(retryCount);
            message.setStatus(OutboxStatus.FAILED);
            message.setNextRetryAt(now.plusSeconds(nextBackoffSeconds(retryCount)));
            message.setUpdatedAt(now);
            outboxMessageMapper.updateById(message);

            if (retryCount >= MAX_RETRY) {
                log.error("Outbox message reached max retry: eventId={}, retryCount={}", message.getEventId(), retryCount, ex);
            } else {
                log.warn("Outbox publish failed: eventId={}, retryCount={}", message.getEventId(), retryCount, ex);
            }
        }
    }

    private long nextBackoffSeconds(int retryCount) {
        long seconds = 5L * (1L << Math.min(retryCount, 6));
        return Math.min(seconds, 300L);
    }

    private Object toEventObject(MqOutboxMessage message) throws Exception {
        if ("order.cancelled".equals(message.getEventType())) {
            return objectMapper.readValue(message.getPayload(), OrderCancelledEvent.class);
        }
        return objectMapper.readValue(message.getPayload(), OrderNotificationEvent.class);
    }
}

