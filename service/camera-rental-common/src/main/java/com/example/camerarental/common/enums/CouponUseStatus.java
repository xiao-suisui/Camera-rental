package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponUseStatus {
    UNUSED("UNUSED"),
    USED("USED"),
    EXPIRED("EXPIRED");

    private final String value;
}

