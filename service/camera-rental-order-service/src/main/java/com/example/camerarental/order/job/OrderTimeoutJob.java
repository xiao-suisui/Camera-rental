package com.example.camerarental.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.camerarental.common.enums.OrderStatus;
import com.example.camerarental.order.entity.Order;
import com.example.camerarental.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 订单超时自动取消定时任务
 * 未支付订单超过 30 分钟自动取消
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderMapper orderMapper;

    /** 每分钟执行一次 */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(30);

        // 先统计数量用于日志
        Long count = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.PENDING_PAY.getValue())
                        .lt(Order::getCreatedAt, deadline)
        );

        if (count == null || count == 0) {
            return;
        }

        // 批量 UPDATE，避免 N+1 问题
        orderMapper.update(null,
                new LambdaUpdateWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.PENDING_PAY.getValue())
                        .lt(Order::getCreatedAt, deadline)
                        .set(Order::getStatus, OrderStatus.CANCELED.getValue())
                        .set(Order::getUpdatedAt, LocalDateTime.now())
        );

        log.info("本次超时取消订单数: {}", count);
    }
}

