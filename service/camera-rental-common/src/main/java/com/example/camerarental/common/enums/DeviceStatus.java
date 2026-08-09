package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备状态
 */
@Getter
@AllArgsConstructor
public enum DeviceStatus {
    ACTIVE("ACTIVE"),     // 在线可租
    OFFLINE("OFFLINE"),   // 已下架
    RENTED("RENTED");     // 已租出

    private final String value;
}

