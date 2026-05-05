package com.example.camerarental.coupon.listener;

import com.example.camerarental.common.enums.CouponUseStatus;
import com.example.camerarental.coupon.entity.UserCoupon;
import com.example.camerarental.coupon.mapper.UserCouponMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserCouponExpirationListener extends KeyExpirationEventMessageListener {

    private static final String KEY_PREFIX = "camera-rental:coupon:user-coupon:";

    private final UserCouponMapper userCouponMapper;

    public UserCouponExpirationListener(RedisMessageListenerContainer listenerContainer,
                                         UserCouponMapper userCouponMapper) {
        super(listenerContainer);
        this.userCouponMapper = userCouponMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = message.toString();
        if (!key.startsWith(KEY_PREFIX)) {
            return;
        }

        Long userCouponId;
        try {
            userCouponId = Long.parseLong(key.substring(KEY_PREFIX.length()));
        } catch (NumberFormatException e) {
            log.warn("Invalid user coupon expiration key: {}", key);
            return;
        }

        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            return;
        }

        // 幂等：仅处理 UNUSED 状态
        if (!CouponUseStatus.UNUSED.getValue().equals(userCoupon.getUseStatus())) {
            return;
        }

        userCoupon.setUseStatus(CouponUseStatus.EXPIRED.getValue());
        userCouponMapper.updateById(userCoupon);
        log.info("User coupon expired via Redis event: userCouponId={}, userId={}, couponId={}",
                userCouponId, userCoupon.getUserId(), userCoupon.getCouponId());
    }
}
