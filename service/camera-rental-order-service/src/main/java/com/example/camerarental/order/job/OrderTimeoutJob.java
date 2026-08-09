package com.example.camerarental.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.camerarental.common.enums.OrderStatus;
import com.example.camerarental.order.entity.Order;
import com.example.camerarental.order.mapper.OrderMapper;
import com.example.camerarental.order.mq.OrderNotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消定时任务
 * 未支付订单超过 30 分钟自动取消
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderMapper orderMapper;
    private final OrderNotificationProducer orderNotificationProducer;

    /** 每分钟执行一次 */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(30);

        List<Order> timeoutOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.PENDING_PAY.getValue())
                        .lt(Order::getCreatedAt, deadline)
        );

        if (timeoutOrders == null || timeoutOrders.isEmpty()) {
            return;
        }

        int canceledCount = 0;
        for (Order order : timeoutOrders) {
            order.setStatus(OrderStatus.CANCELED.getValue());
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);

            orderNotificationProducer.sendOrderCancelledCompensation(
                order.getOrderNo(),
                order.getUserId(),
                order.getDeviceId(),
                order.getCouponId()
            );
            canceledCount++;
        }

        log.info("本次超时取消订单数: {}", canceledCount);
    }
}

