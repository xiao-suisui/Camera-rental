package com.example.camerarental.user.mq;

import com.example.camerarental.common.mq.MqConstants;
import com.example.camerarental.common.mq.OrderNotificationEvent;
import com.example.camerarental.user.entity.MqConsumeLog;
import com.example.camerarental.user.mapper.MqConsumeLogMapper;
import com.example.camerarental.user.service.MessageService;
import com.example.camerarental.user.service.UserService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationConsumer {

    @Value("${mq.consume-log.consumer-name:${spring.application.name}:order-notification-consumer}")
    private String consumerName;

    private final MqConsumeLogMapper mqConsumeLogMapper;
    private final MessageService messageService;
    private final UserService userService;

    @Transactional(rollbackFor = Exception.class)
    @RabbitListener(queues = MqConstants.USER_NOTIFY_QUEUE)
    public void consume(OrderNotificationEvent event,
                        Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String eventId = event.getEventId();
        try {
            if (eventId == null || eventId.isBlank()) {
                log.error("Discard event without eventId: {}", event);
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            try {
                mqConsumeLogMapper.insert(MqConsumeLog.builder()
                        .eventId(eventId)
                        .consumer(consumerName)
                        .consumedAt(LocalDateTime.now())
                        .build());
            } catch (DuplicateKeyException duplicateKeyException) {
                log.info("Skip duplicate notification event: eventId={}", eventId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            if (event.getTargetRole() != null && !event.getTargetRole().isBlank()) {
                List<Long> userIds = userService.getAdminUserIds();
                messageService.sendBatchMessage(userIds, event.getContent(), event.getMessageType());
            } else {
                messageService.sendMessage(event.getUserId(), event.getContent(), event.getMessageType());
            }
            channel.basicAck(deliveryTag, false);
            log.info("Consumed order notification event: eventId={}, eventType={}", eventId, event.getEventType());
        } catch (Exception ex) {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            log.error("Failed to consume order notification event: eventId={}", eventId, ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}


