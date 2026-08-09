package com.example.camerarental.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent implements Serializable {

    private String eventId;
    private String eventType;
    private Integer eventVersion;
    private LocalDateTime occurredAt;
    private String traceId;
    private String producer;
    private String aggregateId;

    private String orderNo;
    private Long userId;
    private Long deviceId;
    private Long couponId;
}

