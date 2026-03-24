package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeviceStatus {
    ACTIVE("ACTIVE"),
    OFFLINE("OFFLINE");

    private final String value;
}

