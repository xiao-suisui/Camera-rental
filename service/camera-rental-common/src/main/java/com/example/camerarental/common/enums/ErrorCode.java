package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用错误码 1xxx
    SUCCESS(0, "操作成功"),
    SYSTEM_ERROR(1000, "系统异常"),
    PARAM_ERROR(1001, "参数错误"),
    UNAUTHORIZED(1002, "未授权"),
    FORBIDDEN(1003, "无权限"),
    NOT_FOUND(1004, "资源不存在"),

    // 用户模块 2xxx
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_EXISTS(2002, "用户已存在"),
    USER_DISABLED(2003, "用户已被禁用"),
    PASSWORD_ERROR(2004, "密码错误"),
    TOKEN_EXPIRED(2005, "Token已过期"),
    TOKEN_INVALID(2006, "Token无效"),

    // 设备模块 3xxx
    DEVICE_NOT_FOUND(3001, "设备不存在"),
    DEVICE_NOT_AVAILABLE(3002, "设备不可用"),
    DEVICE_AUDIT_PENDING(3003, "设备审核中"),

    // 订单模块 4xxx
    ORDER_NOT_FOUND(4001, "订单不存在"),
    ORDER_STATUS_ERROR(4002, "订单状态错误"),
    ORDER_PAID(4003, "订单已支付"),

    // 优惠券模块 5xxx
    COUPON_NOT_FOUND(5001, "优惠券不存在"),
    COUPON_OUT_OF_STOCK(5002, "优惠券已抢光"),
    COUPON_RECEIVED(5003, "优惠券已领取"),
    COUPON_EXPIRED(5004, "优惠券已过期"),
    COUPON_NOT_MEET_THRESHOLD(5005, "未满足优惠券使用门槛");

    private final Integer code;
    private final String message;
}

