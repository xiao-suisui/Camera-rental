package com.example.camerarental.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.camerarental.common.annotation.OperationLog;
import com.example.camerarental.common.enums.CouponStatus;
import com.example.camerarental.common.enums.CouponUseStatus;
import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.common.mq.OrderCancelledEvent;
import com.example.camerarental.coupon.dto.CouponAvailableDTO;
import com.example.camerarental.coupon.dto.CouponCreateRequest;
import com.example.camerarental.coupon.dto.CouponReceiveRequest;
import com.example.camerarental.coupon.dto.UserCouponDTO;
import com.example.camerarental.coupon.entity.Coupon;
import com.example.camerarental.coupon.entity.UserCoupon;
import com.example.camerarental.coupon.mapper.CouponMapper;
import com.example.camerarental.coupon.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String COUPON_STOCK_KEY = "camera-rental:coupon:stock:";
    private static final String USER_COUPON_EXPIRE_KEY = "camera-rental:coupon:user-coupon:";

    @Transactional
    @OperationLog(type = "COUPON_CREATE", description = "创建优惠券")
    public Coupon create(CouponCreateRequest request) {
        if ("THRESHOLD".equals(request.getType())) {
            if (request.getThreshold() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "满减券必须设置使用门槛");
            }
            if (request.getThreshold().compareTo(request.getDenomination()) <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "使用门槛必须大于优惠面额");
            }
        }

        Coupon coupon = Coupon.builder()
                .type(request.getType())
                .denomination(request.getDenomination())
                .threshold(request.getThreshold())
                .totalCount(request.getTotalCount())
                .usedCount(0)
                .validDays(7)
                .status(CouponStatus.PUBLISHED.getValue())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        couponMapper.insert(coupon);

        // 初始化 Redis 库存
        stringRedisTemplate.opsForValue().set(
                COUPON_STOCK_KEY + coupon.getId(),
                String.valueOf(coupon.getTotalCount())
        );

        log.info("Coupon created: couponId={}", coupon.getId());
        return coupon;
    }

    @Transactional
    @OperationLog(type = "COUPON_RECEIVE", description = "领取优惠券")
    @CacheEvict(value = "coupon", key = "#request.couponId")
    public void receive(Long userId, CouponReceiveRequest request) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未获取到用户身份信息");
        }
        // 1. Redis 预扣库存（原子操作，防超发）
        String stockKey = COUPON_STOCK_KEY + request.getCouponId();
        Long remaining = stringRedisTemplate.opsForValue().decrement(stockKey);
        if (remaining == null || remaining < 0) {
            // Redis key 不存在或库存为负，尝试从 DB 同步
            Coupon stockCoupon = couponMapper.selectById(request.getCouponId());
            if (stockCoupon == null) {
                stringRedisTemplate.opsForValue().increment(stockKey);
                throw new BusinessException(ErrorCode.COUPON_NOT_FOUND, "优惠券不存在");
            }
            long realRemaining = stockCoupon.getTotalCount() - stockCoupon.getUsedCount();
            if (realRemaining <= 0) {
                stringRedisTemplate.opsForValue().increment(stockKey);
                throw new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK);
            }
            // 用 DB 真实库存重置 Redis，并重新扣减
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(realRemaining));
            remaining = stringRedisTemplate.opsForValue().decrement(stockKey);
            if (remaining == null || remaining < 0) {
                stringRedisTemplate.opsForValue().increment(stockKey);
                throw new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK);
            }
        }

        try {
            // 2. 检查是否已领取
            UserCoupon existing = userCouponMapper.selectOne(
                    new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                            .eq(UserCoupon::getCouponId, request.getCouponId())
            );
            if (existing != null) {
                // 回补库存
                stringRedisTemplate.opsForValue().increment(stockKey);
                throw new BusinessException(ErrorCode.COUPON_RECEIVED);
            }

            // 3. 查询优惠券基本信息
            Coupon coupon = couponMapper.selectById(request.getCouponId());
            if (coupon == null || !CouponStatus.PUBLISHED.getValue().equals(coupon.getStatus())) {
                stringRedisTemplate.opsForValue().increment(stockKey);
                throw new BusinessException(ErrorCode.COUPON_NOT_FOUND, "优惠券不存在或已下架");
            }

            // 4. 写入用户优惠券
            UserCoupon userCoupon = UserCoupon.builder()
                    .userId(userId)
                    .couponId(request.getCouponId())
                    .receiveTime(LocalDateTime.now())
                    .expireTime(LocalDateTime.now().plusDays(coupon.getValidDays()))
                    .useStatus(CouponUseStatus.UNUSED.getValue())
                    .build();
            userCouponMapper.insert(userCoupon);

            // 5. 写入 Redis 过期 key，用于监听过期事件
            Duration ttl = Duration.between(LocalDateTime.now(), userCoupon.getExpireTime());
            if (!ttl.isNegative() && !ttl.isZero()) {
                stringRedisTemplate.opsForValue().set(
                        USER_COUPON_EXPIRE_KEY + userCoupon.getId(),
                        "1",
                        ttl
                );
            }

            // 6. 原子更新数据库已领取数（避免并发丢失更新）
            couponMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Coupon>()
                            .eq(Coupon::getId, coupon.getId())
                            .setSql("used_count = used_count + 1")
                            .set(Coupon::getUpdatedAt, LocalDateTime.now())
            );

            log.info("Coupon received: userId={}, couponId={}", userId, request.getCouponId());
        } catch (Exception e) {
            // BusinessException 已在各分支处理了库存回补，其他异常才需要回补
            if (!(e instanceof BusinessException)) {
                stringRedisTemplate.opsForValue().increment(stockKey);
            }
            throw e;
        }
    }

    public List<UserCouponDTO> listUserCoupons(Long userId) {
        return userCouponMapper.selectUserCouponsWithDetail(userId);
    }

    @Cacheable(value = "coupon", key = "#couponId")
    public Coupon getById(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }
        return coupon;
    }

    public List<Coupon> listAvailableCoupons() {
        return couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getStatus, CouponStatus.PUBLISHED.getValue())
                        .apply("used_count < total_count")
                        .orderByDesc(Coupon::getCreatedAt)
        );
    }

    public List<CouponAvailableDTO> listAvailableCoupons(Long userId) {
        if (userId != null) {
            return couponMapper.selectAvailableWithReceived(userId);
        }
        return couponMapper.selectAvailable();
    }

    /**
     * 标记优惠券为已使用（由 order-service 在创建订单后调用）
     */
    @Transactional
    public void consumeCoupon(Long userId, Long couponId, Long orderId) {
        UserCoupon userCoupon = userCouponMapper.selectOne(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId)
                        .eq(UserCoupon::getUseStatus, CouponUseStatus.UNUSED.getValue())
                        .last("limit 1")
        );
        if (userCoupon == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND, "无可用的未使用优惠券");
        }
        if (userCoupon.getExpireTime() != null && userCoupon.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED, "优惠券已过期");
        }

        userCoupon.setUseStatus(CouponUseStatus.USED.getValue());
        userCoupon.setOrderId(orderId);
        userCouponMapper.updateById(userCoupon);

        stringRedisTemplate.delete(USER_COUPON_EXPIRE_KEY + userCoupon.getId());

        log.info("Coupon consumed: userId={}, couponId={}, orderId={}", userId, couponId, orderId);
    }

    // ========== 管理员方法 ==========

    public List<Coupon> listAllCoupons() {
        return couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>().orderByDesc(Coupon::getCreatedAt)
        );
    }

    @Transactional
    @OperationLog(type = "COUPON_OFFLINE", description = "下架优惠券")
    @CacheEvict(value = "coupon", key = "#couponId")
    public void offlineCoupon(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }
        coupon.setStatus(CouponStatus.OFFLINE.getValue());
        coupon.setUpdatedAt(LocalDateTime.now());
        couponMapper.updateById(coupon);
        log.info("Coupon offline: couponId={}", couponId);
    }

    @Transactional
    public void handleOrderCancelledRollbackCoupon(OrderCancelledEvent event) {
        if (event == null || event.getCouponId() == null || event.getUserId() == null) {
            log.warn("Skip coupon rollback for invalid order-cancelled event: {}", event);
            return;
        }

        UserCoupon userCoupon = userCouponMapper.selectOne(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, event.getUserId())
                        .eq(UserCoupon::getCouponId, event.getCouponId())
                        .eq(UserCoupon::getUseStatus, CouponUseStatus.USED.getValue())
                        .last("limit 1")
        );
        if (userCoupon == null) {
            log.info("No used coupon needs rollback: userId={}, couponId={}", event.getUserId(), event.getCouponId());
            return;
        }

        userCoupon.setUseStatus(CouponUseStatus.UNUSED.getValue());
        userCoupon.setOrderId(null);
        userCouponMapper.updateById(userCoupon);

        // 回补 Redis 库存（领券时已扣减，取消订单后恢复可领取数量）
        stringRedisTemplate.opsForValue().increment(COUPON_STOCK_KEY + event.getCouponId());
        log.info("Rolled back coupon usage for cancelled order: orderNo={}, userId={}, couponId={}",
                event.getOrderNo(), event.getUserId(), event.getCouponId());
    }
}

