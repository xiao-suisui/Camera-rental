package com.example.camerarental.coupon.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.camerarental.common.enums.CouponUseStatus;
import com.example.camerarental.coupon.entity.UserCoupon;
import com.example.camerarental.coupon.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCouponExpirationJob {

    private final UserCouponMapper userCouponMapper;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireUserCoupons() {
        List<UserCoupon> expiredCoupons = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUseStatus, CouponUseStatus.UNUSED.getValue())
                        .lt(UserCoupon::getExpireTime, LocalDateTime.now())
                        .last("LIMIT 500")
        );

        if (expiredCoupons.isEmpty()) {
            return;
        }

        for (UserCoupon userCoupon : expiredCoupons) {
            userCoupon.setUseStatus(CouponUseStatus.EXPIRED.getValue());
            userCouponMapper.updateById(userCoupon);
        }

        log.info("UserCouponExpirationJob: expired {} user coupons", expiredCoupons.size());
    }
}
