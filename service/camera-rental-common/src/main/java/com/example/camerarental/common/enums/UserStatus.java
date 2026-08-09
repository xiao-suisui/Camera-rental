package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态
 */
@Getter
@AllArgsConstructor
public enum UserStatus {
    ACTIVE("ACTIVE"),       // 正常
    DISABLED("DISABLED");   // 已禁用

    private final String value;
}

