package com.example.camerarental.coupon.controller;

import com.example.camerarental.common.annotation.AdminOnly;
import com.example.camerarental.common.annotation.RepeatSubmit;
import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.common.result.Result;
import com.example.camerarental.coupon.dto.CouponCreateRequest;
import com.example.camerarental.coupon.dto.CouponReceiveRequest;
import com.example.camerarental.coupon.dto.UserCouponDTO;
import com.example.camerarental.coupon.entity.Coupon;
import com.example.camerarental.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @AdminOnly
    @PostMapping("/create")
    public Result<Coupon> create(@Valid @RequestBody CouponCreateRequest request) {
        return Result.success(couponService.create(request));
    }

    @PostMapping("/receive")
    @RepeatSubmit(interval = 3, message = "请勿重复领取")
    public Result<Void> receive(
            @Valid @RequestBody CouponReceiveRequest request,
            @RequestHeader("X-User-Id") Long operatorUserId) {
        couponService.receive(operatorUserId, request);
        return Result.success("receive success", null);
    }

    @GetMapping("/user/me")
    public Result<List<UserCouponDTO>> listMyCoupons(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(couponService.listUserCoupons(userId));
    }

    @GetMapping("/user/{userId}")
    public Result<List<UserCouponDTO>> listUserCoupons(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long operatorUserId,
            @RequestHeader(value = "X-User-Role", required = false) String operatorRole) {
        if (!"ADMIN".equalsIgnoreCase(operatorRole)
                && (operatorUserId == null || !operatorUserId.equals(userId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看其他用户优惠券");
        }
        return Result.success(couponService.listUserCoupons(userId));
    }

    @GetMapping("/{couponId}")
    public Result<Coupon> getById(@PathVariable Long couponId) {
        return Result.success(couponService.getById(couponId));
    }

    @GetMapping("/available")
    public Result<List<Coupon>> listAvailable() {
        return Result.success(couponService.listAvailableCoupons());
    }

    @PostMapping("/consume")
    public Result<Void> consumeCoupon(
            @RequestParam("couponId") Long couponId,
            @RequestParam("orderId") Long orderId,
            @RequestParam("userId") Long userId) {
        couponService.consumeCoupon(userId, couponId, orderId);
        return Result.success("consume success", null);
    }

    // ========== 管理员接口 ==========

    @AdminOnly
    @GetMapping("/admin/list")
    public Result<List<Coupon>> listAllCoupons() {
        return Result.success(couponService.listAllCoupons());
    }

    @AdminOnly
    @PutMapping("/admin/{couponId}/offline")
    public Result<Void> offlineCoupon(@PathVariable Long couponId) {
        couponService.offlineCoupon(couponId);
        return Result.success("优惠券已下架", null);
    }
}

