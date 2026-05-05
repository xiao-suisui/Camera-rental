package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeviceStatus {
    ACTIVE("ACTIVE"),
    OFFLINE("OFFLINE"),
    RENTED("RENTED");

    private final String value;
}

