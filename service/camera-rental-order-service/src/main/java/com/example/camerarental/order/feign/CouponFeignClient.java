package com.example.camerarental.order.feign;

import com.example.camerarental.common.result.Result;
import com.example.camerarental.order.dto.CouponDTO;
import com.example.camerarental.order.fallback.CouponFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "camera-rental-coupon-service",
    path = "/coupon",
    fallback = CouponFeignClientFallback.class
)
public interface CouponFeignClient {

    @GetMapping("/{couponId}")
    Result<CouponDTO> getById(@PathVariable("couponId") Long couponId);

    @PostMapping("/consume")
    Result<Void> consumeCoupon(@RequestParam("couponId") Long couponId,
                                @RequestParam("orderId") Long orderId,
                                @RequestParam("userId") Long userId);
}



