package com.example.camerarental.order.feign;

import com.example.camerarental.common.result.Result;
import com.example.camerarental.order.dto.CouponDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "camera-rental-coupon-service", path = "/coupon")
public interface CouponFeignClient {

    @GetMapping("/{couponId}")
    Result<CouponDTO> getById(@PathVariable("couponId") Long couponId);
}



