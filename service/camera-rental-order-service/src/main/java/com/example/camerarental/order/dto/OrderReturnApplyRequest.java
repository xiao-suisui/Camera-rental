package com.example.camerarental.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderReturnApplyRequest {

    @NotBlank(message = "归还物流单号不能为空")
    @Size(max = 64, message = "归还物流单号长度不能超过64")
    private String trackingNumber;
}

