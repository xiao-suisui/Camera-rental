package com.example.camerarental.order.feign;

import com.example.camerarental.common.result.Result;
import com.example.camerarental.order.fallback.MessageFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "camera-rental-user-service",
        path = "/message",
        fallback = MessageFeignClientFallback.class
)
public interface MessageFeignClient {

    @PostMapping("/internal/send")
    Result<Void> sendInternalMessage(
            @RequestParam("userId") Long userId,
            @RequestParam("content") String content,
            @RequestParam("type") String type);

    @PostMapping("/internal/send-to-admins")
    Result<Void> sendToAllAdmins(
            @RequestParam("content") String content,
            @RequestParam("type") String type);
}
