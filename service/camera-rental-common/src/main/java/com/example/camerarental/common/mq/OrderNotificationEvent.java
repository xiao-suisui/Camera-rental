package com.example.camerarental.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Unified event for order-related user notifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderNotificationEvent implements Serializable {

    private String eventId;
    private String eventType;
    private Integer eventVersion;
    private LocalDateTime occurredAt;
    private String traceId;
    private String producer;
    private String aggregateId;

    private Long userId;
    private String content;
    private String messageType;
}

