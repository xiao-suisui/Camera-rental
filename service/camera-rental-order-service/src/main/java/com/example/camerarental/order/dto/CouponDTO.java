package com.example.camerarental.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券信息DTO（用于Feign调用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDTO {
    private Long id;
    private String type;
    private BigDecimal denomination;
    private BigDecimal threshold;
    private String status;
    private LocalDateTime expireTime;
}

