package com.example.camerarental.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponCreateRequest {
    @NotBlank(message = "type is required")
    private String type;

    @NotNull(message = "denomination is required")
    @DecimalMin(value = "0.01")
    private BigDecimal denomination;

    private BigDecimal threshold;

    @NotNull(message = "totalCount is required")
    @Min(value = 1)
    private Integer totalCount;
}

