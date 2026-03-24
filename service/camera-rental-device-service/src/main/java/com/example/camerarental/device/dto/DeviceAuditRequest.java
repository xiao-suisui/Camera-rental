package com.example.camerarental.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceAuditRequest {
    @NotNull(message = "deviceId is required")
    private Long deviceId;

    @NotBlank(message = "auditStatus is required")
    private String auditStatus;

    private String rejectReason;
}

