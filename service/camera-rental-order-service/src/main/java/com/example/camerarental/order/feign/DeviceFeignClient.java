package com.example.camerarental.order.feign;

import com.example.camerarental.common.result.Result;
import com.example.camerarental.order.dto.DeviceDTO;
import com.example.camerarental.order.fallback.DeviceFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "camera-rental-device-service",
        path = "/device",
        fallback = DeviceFeignClientFallback.class
)
public interface DeviceFeignClient {

    @GetMapping("/{deviceId}")
    Result<DeviceDTO> getById(@PathVariable("deviceId") Long deviceId);

    @PostMapping("/{deviceId}/lock")
    Result<Void> lockDevice(@PathVariable("deviceId") Long deviceId);

    @PostMapping("/{deviceId}/unlock")
    Result<Void> unlockDevice(@PathVariable("deviceId") Long deviceId);
}

