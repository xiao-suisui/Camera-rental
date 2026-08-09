package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 纠纷状态
 */
@Getter
@AllArgsConstructor
public enum DisputeStatus {
    SUBMITTED("SUBMITTED"),     // 已提交
    PROCESSING("PROCESSING"),   // 处理中
    RESOLVED("RESOLVED"),       // 已解决
    REJECTED("REJECTED");       // 已驳回

    private final String value;
}
