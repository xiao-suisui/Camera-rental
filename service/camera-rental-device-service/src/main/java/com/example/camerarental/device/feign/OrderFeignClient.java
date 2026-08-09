package com.example.camerarental.device.feign;

import com.example.camerarental.common.result.Result;
import com.example.camerarental.device.fallback.OrderFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "camera-rental-order-service",
        path = "/order",
        fallback = OrderFeignClientFallback.class
)
public interface OrderFeignClient {

    @GetMapping("/device/{deviceId}/has-active")
    Result<Boolean> hasActiveOrders(@PathVariable("deviceId") Long deviceId);
}
