package com.example.camerarental.order.mq;

import com.example.camerarental.common.mq.MqConstants;
import com.example.camerarental.common.mq.OrderCancelledEvent;
import com.example.camerarental.common.mq.OrderNotificationEvent;
import com.example.camerarental.order.entity.MqOutboxMessage;
import com.example.camerarental.order.mapper.MqOutboxMessageMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationProducer {

    private static final String PRODUCER = "camera-rental-order-service";

    private final MqOutboxMessageMapper outboxMessageMapper;
    private final ObjectMapper objectMapper;

    public void sendCreatedNotification(String orderNo, Long userId, String content) {
        send(MqConstants.ORDER_CREATED_NOTIFY_USER_KEY, "order.created", orderNo, userId, content);
    }

    public void sendPaidNotification(String orderNo, Long userId, String content) {
        send(MqConstants.ORDER_PAID_NOTIFY_USER_KEY, "order.paid", orderNo, userId, content);
    }

    public void sendShippedNotification(String orderNo, Long userId, String content) {
        send(MqConstants.ORDER_SHIPPED_NOTIFY_USER_KEY, "order.shipped", orderNo, userId, content);
    }

    public void sendCancelledNotification(String orderNo, Long userId, String content) {
        send(MqConstants.ORDER_CANCELLED_NOTIFY_USER_KEY, "order.cancelled", orderNo, userId, content);
    }

    public void sendReturnAppliedNotification(String orderNo, Long userId, String content) {
        send(MqConstants.ORDER_RETURN_APPLIED_NOTIFY_USER_KEY, "order.return.applied", orderNo, userId, content);
    }

    public void sendReturnConfirmedNotification(String orderNo, Long userId, String content) {
        send(MqConstants.ORDER_RETURN_CONFIRMED_NOTIFY_USER_KEY, "order.return.confirmed", orderNo, userId, content);
    }

    public void sendReceivedNotification(String orderNo, Long userId, String content) {
        send(MqConstants.ORDER_RECEIVED_NOTIFY_USER_KEY, "order.received", orderNo, userId, content);
    }

    public void sendDisputeCreatedNotifyAdmins(String orderNo, String content) {
        String eventId = UUID.randomUUID().toString().replace("-", "");

        OrderNotificationEvent event = OrderNotificationEvent.builder()
                .eventId(eventId)
                .eventType("dispute.created")
                .eventVersion(1)
                .occurredAt(LocalDateTime.now())
                .traceId(eventId)
                .producer(PRODUCER)
                .aggregateId(orderNo)
                .userId(null)
                .targetRole("ADMIN")
                .content(content)
                .messageType("SYSTEM")
                .build();

        saveOutbox(eventId, "dispute.created", orderNo,
                MqConstants.DISPUTE_CREATED_NOTIFY_ADMIN_KEY, toJson(event));

        log.info("Outbox dispute admin notification created: eventId={}, orderNo={}", eventId, orderNo);
    }

    public void sendDisputeResolvedNotifyUser(String orderNo, Long userId, String content) {
        send(MqConstants.DISPUTE_RESOLVED_NOTIFY_USER_KEY, "dispute.resolved", orderNo, userId, content);
    }

    public void sendOrderCancelledCompensation(String orderNo, Long userId, Long deviceId, Long couponId) {
        String eventId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .eventId(eventId)
                .eventType("order.cancelled")
                .eventVersion(1)
                .occurredAt(now)
                .traceId(eventId)
                .producer(PRODUCER)
                .aggregateId(orderNo)
                .orderNo(orderNo)
                .userId(userId)
                .deviceId(deviceId)
                .couponId(couponId)
                .build();

        saveOutbox(eventId, "order.cancelled", orderNo, MqConstants.ORDER_CANCELLED_RELEASE_STOCK_KEY, toJson(event));
        if (couponId != null) {
            saveOutbox(eventId + "-coupon", "order.cancelled", orderNo,
                    MqConstants.ORDER_CANCELLED_ROLLBACK_COUPON_KEY, toJson(event));
        }
    }

    private void send(String routingKey, String eventType, String orderNo, Long userId, String content) {
        String eventId = UUID.randomUUID().toString().replace("-", "");

        OrderNotificationEvent event = OrderNotificationEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .eventVersion(1)
                .occurredAt(LocalDateTime.now())
                .traceId(eventId)
                .producer(PRODUCER)
                .aggregateId(orderNo)
                .userId(userId)
                .content(content)
                .messageType("ORDER_STATUS")
                .build();

        String payload = toJson(event);
        saveOutbox(eventId, eventType, orderNo, routingKey, payload);

        log.info("Outbox event created: eventId={}, eventType={}, orderNo={}, userId={}",
                eventId, eventType, orderNo, userId);
    }

    private void saveOutbox(String eventId, String eventType, String orderNo, String routingKey, String payload) {
        LocalDateTime now = LocalDateTime.now();
        MqOutboxMessage outboxMessage = MqOutboxMessage.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateId(orderNo)
                .routingKey(routingKey)
                .payload(payload)
                .status(OutboxStatus.NEW)
                .retryCount(0)
                .nextRetryAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        outboxMessageMapper.insert(outboxMessage);
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize mq event", ex);
        }
    }
}



