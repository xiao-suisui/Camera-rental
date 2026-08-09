package com.example.camerarental.coupon.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponAvailableDTO {
    private Long id;
    private String type;
    private BigDecimal denomination;
    private BigDecimal threshold;
    private Integer totalCount;
    private Integer usedCount;
    private Integer validDays;
    private String status;
    private LocalDateTime createdAt;
    private Boolean received;
}
