package com.example.camerarental.order.feign;

import com.example.camerarental.common.result.Result;
import com.example.camerarental.order.dto.DeviceDTO;
import com.example.camerarental.order.fallback.DeviceFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "camera-rental-device-service",
        path = "/device",
        fallback = DeviceFeignClientFallback.class
)
public interface DeviceFeignClient {

    @GetMapping("/{deviceId}")
    Result<DeviceDTO> getById(@PathVariable("deviceId") Long deviceId);
}

