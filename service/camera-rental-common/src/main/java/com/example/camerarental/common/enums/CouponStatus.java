package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优惠券状态
 */
@Getter
@AllArgsConstructor
public enum CouponStatus {
    DRAFT("DRAFT"),           // 草稿
    PUBLISHED("PUBLISHED"),   // 已发布
    EXPIRED("EXPIRED"),       // 已过期
    OFFLINE("OFFLINE");       // 已下架

    private final String value;
}

