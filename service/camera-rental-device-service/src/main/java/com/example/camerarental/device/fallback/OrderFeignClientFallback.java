package com.example.camerarental.device.fallback;

import com.example.camerarental.common.result.Result;
import com.example.camerarental.device.feign.OrderFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderFeignClientFallback implements OrderFeignClient {

    @Override
    public Result<Boolean> hasActiveOrders(Long deviceId) {
        log.error("调用Order服务失败(检查活跃订单)，deviceId={}", deviceId);
        return Result.error("订单服务暂时不可用，请稍后重试");
    }
}
