package com.example.camerarental.order.feign;

import com.example.camerarental.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 消息服务Feign客户端
 */
@FeignClient(name = "camera-rental-user-service", contextId = "messageFeignClient")
public interface MessageFeignClient {

    /**
     * 发送站内信
     */
    @PostMapping("/message/send")
    Result<Void> sendMessage(
            @RequestParam("userId") Long userId,
            @RequestParam("content") String content,
            @RequestParam("type") String type
    );
}

