package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponStatus {
    DRAFT("DRAFT"),
    PUBLISHED("PUBLISHED"),
    EXPIRED("EXPIRED"),
    OFFLINE("OFFLINE");

    private final String value;
}

