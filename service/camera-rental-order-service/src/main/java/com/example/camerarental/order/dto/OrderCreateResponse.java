package com.example.camerarental.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderCreateResponse {
    private String orderNo;
    private BigDecimal totalRent;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
}

