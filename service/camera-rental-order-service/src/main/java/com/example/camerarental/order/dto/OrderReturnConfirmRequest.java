package com.example.camerarental.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderReturnConfirmRequest {

    @NotBlank(message = "设备状态说明不能为空")
    @Size(max = 255, message = "设备状态说明长度不能超过255")
    private String condition;
}

