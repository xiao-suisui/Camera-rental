package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户优惠券使用状态
 */
@Getter
@AllArgsConstructor
public enum CouponUseStatus {
    UNUSED("UNUSED"),     // 未使用
    USED("USED"),         // 已使用
    EXPIRED("EXPIRED");   // 已过期

    private final String value;
}

