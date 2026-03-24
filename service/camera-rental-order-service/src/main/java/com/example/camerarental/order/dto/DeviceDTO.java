package com.example.camerarental.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 设备信息DTO（用于Feign调用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDTO {
    private Long id;
    private Long userId;
    private BigDecimal rentPrice;
    private BigDecimal deposit;
    private String status;
}

