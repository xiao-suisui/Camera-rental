package com.example.camerarental.device.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DevicePublishResponse {
    private Long deviceId;
    private String auditStatus;
}

