package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DepositStatus {
    UNPAID("UNPAID"),
    PAID("PAID"),
    REFUNDED("REFUNDED");

    private final String value;
}

