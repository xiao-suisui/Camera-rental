package com.example.camerarental.order.fallback;

import com.example.camerarental.common.result.Result;
import com.example.camerarental.order.feign.MessageFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageFeignClientFallback implements MessageFeignClient {

    @Override
    public Result<Void> sendInternalMessage(Long userId, String content, String type) {
        log.error("调用Message服务失败(内部通知)，userId={}, content={}", userId, content);
        return Result.success("通知服务暂时不可用", null);
    }

    @Override
    public Result<Void> sendToAllAdmins(String content, String type) {
        log.error("调用Message服务失败(管理员通知)，content={}", content);
        return Result.success("通知服务暂时不可用", null);
    }
}
