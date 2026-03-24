package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponStatus {
    PUBLISHED("PUBLISHED"),
    OFFLINE("OFFLINE");

    private final String value;
}

