package com.example.camerarental.order.fallback;

import com.example.camerarental.common.result.Result;
import com.example.camerarental.order.dto.CouponDTO;
import com.example.camerarental.order.feign.CouponFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Coupon服务降级处理
 */
@Slf4j
@Component
public class CouponFeignClientFallback implements CouponFeignClient {

    @Override
    public Result<CouponDTO> getById(Long couponId) {
        log.error("调用Coupon服务失败，couponId={}", couponId);
        return Result.error("优惠券服务暂时不可用，请稍后重试");
    }
}
