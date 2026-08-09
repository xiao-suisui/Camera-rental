package com.example.camerarental.order.fallback;

import com.example.camerarental.common.result.Result;
import com.example.camerarental.order.dto.DeviceDTO;
import com.example.camerarental.order.feign.DeviceFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Device服务降级处理
 */
@Slf4j
@Component
public class DeviceFeignClientFallback implements DeviceFeignClient {

    @Override
    public Result<DeviceDTO> getById(Long deviceId) {
        log.error("调用Device服务失败，deviceId={}", deviceId);
        return Result.error("设备服务暂时不可用，请稍后重试");
    }

    @Override
    public Result<Void> lockDevice(Long deviceId) {
        log.error("调用Device服务失败(锁定设备)，deviceId={}", deviceId);
        return Result.error("设备服务暂时不可用，请稍后重试");
    }

    @Override
    public Result<Void> unlockDevice(Long deviceId) {
        log.error("调用Device服务失败(解锁设备)，deviceId={}", deviceId);
        return Result.error("设备服务暂时不可用，请稍后重试");
    }
}

