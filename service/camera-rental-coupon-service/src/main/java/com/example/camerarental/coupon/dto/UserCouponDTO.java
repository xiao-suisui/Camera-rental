package com.example.camerarental.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponDTO {
    private Long id;
    private Long userId;
    private Long couponId;
    private LocalDateTime receiveTime;
    private LocalDateTime expireTime;
    private String useStatus;
    private Long orderId;
    
    private BigDecimal denomination;
    private BigDecimal threshold;
    private String type;
}