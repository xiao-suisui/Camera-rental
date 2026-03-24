package com.example.camerarental.common.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;

import java.time.LocalDateTime;

/**
 * 业务工具类
 */
public class BusinessUtils {

    /**
     * 生成订单号
     */
    public static String generateOrderNo() {
        return "ORD" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss") + IdUtil.randomUUID().substring(0, 6).toUpperCase();
    }

    /**
     * 生成批次号
     */
    public static String generateBatchId() {
        return "BATCH" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss");
    }

    /**
     * 生成优惠券编码
     */
    public static String generateCouponCode() {
        return "CPN" + DateUtil.format(LocalDateTime.now(), "yyyyMMdd") + IdUtil.randomUUID().substring(0, 8).toUpperCase();
    }
}

