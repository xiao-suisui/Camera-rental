package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备审核状态
 */
@Getter
@AllArgsConstructor
public enum AuditStatus {
    PENDING("PENDING"),     // 待审核
    APPROVED("APPROVED"),   // 已通过
    REJECTED("REJECTED");   // 已拒绝

    private final String value;
}

