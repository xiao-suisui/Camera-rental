package com.example.camerarental.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 纠纷类型
 */
@Getter
@AllArgsConstructor
public enum DisputeType {
    DEVICE_DAMAGE("DEVICE_DAMAGE"),               // 设备损坏
    DEPOSIT_DISPUTE("DEPOSIT_DISPUTE"),           // 押金纠纷
    OVERDUE("OVERDUE"),                           // 逾期纠纷
    DESCRIPTION_MISMATCH("DESCRIPTION_MISMATCH"), // 描述不符
    OTHER("OTHER");                               // 其他

    private final String value;
}
