package com.example.camerarental.device.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DeviceBatchPublishRequest {
    @NotNull(message = "设备标题不能为空")
    private String title;

    @NotNull(message = "品牌不能为空")
    private String brand;

    @NotNull(message = "型号不能为空")
    private String model;

    @NotNull(message = "描述不能为空")
    private String description;

    @NotNull(message = "图片不能为空")
    private List<String> imagePaths;

    @NotNull(message = "租金不能为空")
    private BigDecimal rentPrice;

    @NotNull(message = "押金不能为空")
    private BigDecimal deposit;

    @NotNull(message = "地址不能为空")
    private String address;

    @NotNull(message = "发布数量不能为空")
    @Min(value = 1, message = "至少发布1台")
    @Max(value = 10, message = "最多发布10台")
    private Integer quantity;
}

