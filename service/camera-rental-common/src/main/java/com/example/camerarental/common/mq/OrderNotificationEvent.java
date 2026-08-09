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

    /**
     * 按角色广播时使用（如通知所有管理员）。
     * 当 targetRole 非空且 userId 为空时，消费端按角色查询用户列表并批量发送。
     */
    private String targetRole;
}

