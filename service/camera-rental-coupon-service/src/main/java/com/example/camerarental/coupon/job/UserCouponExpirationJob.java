package com.example.camerarental.coupon.job;

import com.example.camerarental.common.enums.CouponUseStatus;
import com.example.camerarental.coupon.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCouponExpirationJob {

    private final UserCouponMapper userCouponMapper;

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void expireUserCoupons() {
        int affected = userCouponMapper.batchExpireCoupons(
                CouponUseStatus.EXPIRED.getValue(),
                CouponUseStatus.UNUSED.getValue(),
                LocalDateTime.now(),
                100
        );

        if (affected > 0) {
            log.info("UserCouponExpirationJob: expired {} user coupons", affected);
        }
    }
}
