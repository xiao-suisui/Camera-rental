package com.example.camerarental.device.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DevicePublishRequest {
    @NotBlank(message = "title is required")
    @Size(max = 128)
    private String title;

    @NotBlank(message = "brand is required")
    @Size(max = 64)
    private String brand;

    @NotBlank(message = "model is required")
    @Size(max = 64)
    private String model;

    @NotBlank(message = "description is required")
    @Size(min = 50, max = 500)
    private String description;

    @NotNull(message = "imagePaths is required")
    @Size(min = 1, max = 5)
    private List<String> imagePaths;

    @NotNull(message = "rentPrice is required")
    @DecimalMin(value = "0.01")
    private BigDecimal rentPrice;

    @NotNull(message = "deposit is required")
    @DecimalMin(value = "0.00")
    private BigDecimal deposit;

    @NotBlank(message = "address is required")
    @Size(max = 128)
    private String address;
}

