package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {
    PENDING_PAY("PENDING_PAY"),   // 待支付
    PAID("PAID"),                 // 已支付
    TO_SHIP("TO_SHIP"),           // 待发货
    TO_RETURN("TO_RETURN"),       // 待归还
    RETURNED("RETURNED"),         // 已归还
    OVERDUE("OVERDUE"),           // 已逾期
    FINISHED("FINISHED"),         // 已完成
    CANCELED("CANCELED");         // 已取消

    private final String value;
}
