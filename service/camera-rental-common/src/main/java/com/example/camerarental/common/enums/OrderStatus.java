package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {
    PENDING_PAY("PENDING_PAY"),
    PAID("PAID"),
    TO_SHIP("TO_SHIP"),
    TO_RETURN("TO_RETURN"),
    RETURNED("RETURNED"),
    OVERDUE("OVERDUE"),
    FINISHED("FINISHED"),
    CANCELED("CANCELED");

    private final String value;
}
