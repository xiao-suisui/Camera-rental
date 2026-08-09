package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 押金状态
 */
@Getter
@AllArgsConstructor
public enum DepositStatus {
    UNPAID("UNPAID"),       // 未支付
    PAID("PAID"),           // 已支付
    REFUNDED("REFUNDED");   // 已退还

    private final String value;
}

