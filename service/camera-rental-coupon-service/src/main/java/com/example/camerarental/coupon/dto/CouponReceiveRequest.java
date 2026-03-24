package com.example.camerarental.coupon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CouponReceiveRequest {
    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "couponId is required")
    private Long couponId;
}

