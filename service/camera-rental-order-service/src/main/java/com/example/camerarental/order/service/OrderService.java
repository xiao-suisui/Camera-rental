package com.example.camerarental.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.camerarental.common.annotation.OperationLog;
import com.example.camerarental.common.enums.DepositStatus;
import com.example.camerarental.common.enums.DeviceStatus;
import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.enums.OrderStatus;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.order.dto.CouponDTO;
import com.example.camerarental.order.dto.DeviceDTO;
import com.example.camerarental.order.dto.OrderCreateRequest;
import com.example.camerarental.order.dto.OrderCreateResponse;
import com.example.camerarental.order.entity.Order;
import com.example.camerarental.order.feign.CouponFeignClient;
import com.example.camerarental.order.feign.DeviceFeignClient;
import com.example.camerarental.order.feign.MessageFeignClient;
import com.example.camerarental.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final DeviceFeignClient deviceFeignClient;
    private final CouponFeignClient couponFeignClient;
    private final MessageFeignClient messageFeignClient;

    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponse createOrder(OrderCreateRequest request, Long operatorUserId, String operatorRole) {
        checkOperatorPermission(operatorUserId, operatorRole, request.getUserId());

        // 1. 查询设备信息
        DeviceDTO device = deviceFeignClient.getById(request.getDeviceId()).getData();
        if (device == null) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }
        if (!DeviceStatus.ACTIVE.getValue().equals(device.getStatus())) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_AVAILABLE);
        }

        // 2. 计算租金
        long days = ChronoUnit.DAYS.between(request.getStartTime().toLocalDate(),
                request.getEndTime().toLocalDate()) + 1;
        BigDecimal totalRent = device.getRentPrice().multiply(BigDecimal.valueOf(days));
        BigDecimal discountAmount = BigDecimal.ZERO;

        // 3. 应用优惠券
        if (request.getCouponId() != null) {
            CouponDTO coupon = couponFeignClient.getById(request.getCouponId()).getData();
            if (coupon != null) {
                if (coupon.getThreshold() != null && totalRent.compareTo(coupon.getThreshold()) < 0) {
                    throw new BusinessException(ErrorCode.COUPON_NOT_MEET_THRESHOLD);
                }
                discountAmount = coupon.getDenomination();
            }
        }

        BigDecimal payAmount = totalRent.subtract(discountAmount);
        if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
            payAmount = BigDecimal.ZERO;
        }

        // 4. 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(request.getUserId());
        order.setDeviceId(request.getDeviceId());
        order.setLessorId(device.getUserId());
        order.setStartTime(request.getStartTime());
        order.setEndTime(request.getEndTime());
        order.setTotalRent(totalRent);
        order.setDeposit(device.getDeposit());
        order.setCouponId(request.getCouponId());
        order.setDiscountAmount(discountAmount);
        order.setPayAmount(payAmount);
        order.setStatus(OrderStatus.PENDING_PAY.getValue());
        order.setDepositStatus(DepositStatus.UNPAID.getValue());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        orderMapper.insert(order);
        log.info("Order created: orderNo={}", order.getOrderNo());

        return new OrderCreateResponse(order.getOrderNo(), totalRent, discountAmount, payAmount);
    }

    @Transactional
    @OperationLog(type = "ORDER_PAY", description = "订单支付")
    public void mockPay(Long orderId, Long operatorUserId, String operatorRole) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        checkOperatorPermission(operatorUserId, operatorRole, order.getUserId());
        if (!OrderStatus.PENDING_PAY.getValue().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态异常，无法支付");
        }

        order.setStatus(OrderStatus.PAID.getValue());
        order.setDepositStatus(DepositStatus.PAID.getValue());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("Order paid: orderId={}", orderId);
    }

    @Transactional
    @OperationLog(type = "ORDER_SHIP", description = "确认发货")
    public void ship(Long orderId, String trackingNumber, Long operatorUserId, String operatorRole) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单不存在");
        }
        checkOperatorPermission(operatorUserId, operatorRole, order.getLessorId());
        if (!OrderStatus.PAID.getValue().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单状态错误，无法发货");
        }

        order.setStatus(OrderStatus.TO_SHIP.getValue());
        order.setVirtualOrderNo(trackingNumber);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("Order shipped: orderId={}, trackingNumber={}", orderId, trackingNumber);

        // 发送站内信通知租用者
        try {
            messageFeignClient.sendMessage(
                    order.getUserId(),
                    String.format("您的订单 %s 已发货，物流单号：%s", order.getOrderNo(), trackingNumber),
                    "ORDER_STATUS"
            );
        } catch (Exception e) {
            log.error("Failed to send message", e);
        }
    }

    @Transactional
    @OperationLog(type = "ORDER_RECEIVE", description = "确认收货")
    public void receive(Long orderId, Long operatorUserId, String operatorRole) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单不存在");
        }
        checkOperatorPermission(operatorUserId, operatorRole, order.getUserId());
        if (!OrderStatus.TO_SHIP.getValue().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单状态错误，无法确认收货");
        }

        order.setStatus(OrderStatus.TO_RETURN.getValue());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("Order received: orderId={}", orderId);

        try {
            messageFeignClient.sendMessage(
                    order.getLessorId(),
                    String.format("租用者已确认收货，订单 %s 进入使用中", order.getOrderNo()),
                    "ORDER_STATUS"
            );
        } catch (Exception e) {
            log.error("Failed to send message", e);
        }
    }

    @Transactional
    @OperationLog(type = "ORDER_RETURN", description = "申请归还")
    public void applyReturn(Long orderId, String trackingNumber, Long operatorUserId, String operatorRole) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单不存在");
        }
        checkOperatorPermission(operatorUserId, operatorRole, order.getUserId());
        if (!OrderStatus.TO_RETURN.getValue().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单状态错误，无法申请归还");
        }

        // 检查是否逾期
        if (LocalDateTime.now().isAfter(order.getEndTime())) {
            order.setStatus(OrderStatus.OVERDUE.getValue());
        } else {
            order.setStatus(OrderStatus.RETURNED.getValue());
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("Order return applied: orderId={}, trackingNumber={}", orderId, trackingNumber);

        // 发送站内信通知出租者
        try {
            messageFeignClient.sendMessage(
                    order.getLessorId(),
                    String.format("租用者已申请归还订单 %s，请注意查收", order.getOrderNo()),
                    "ORDER_STATUS"
            );
        } catch (Exception e) {
            log.error("Failed to send message", e);
        }
    }

    @Transactional
    @OperationLog(type = "ORDER_CONFIRM", description = "确认归还")
    public void confirmReturn(Long orderId, String condition, Long operatorUserId, String operatorRole) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单不存在");
        }
        checkOperatorPermission(operatorUserId, operatorRole, order.getLessorId());
        if (!OrderStatus.RETURNED.getValue().equals(order.getStatus())
                && !OrderStatus.OVERDUE.getValue().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单状态错误，无法确认归还");
        }

        order.setStatus(OrderStatus.FINISHED.getValue());
        order.setDepositStatus(DepositStatus.REFUNDED.getValue());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("Order return confirmed: orderId={}, condition={}", orderId, condition);

        // 发送站内信通知租用者
        try {
            messageFeignClient.sendMessage(
                    order.getUserId(),
                    String.format("您的订单 %s 已确认归还，押金已退还", order.getOrderNo()),
                    "ORDER_STATUS"
            );
        } catch (Exception e) {
            log.error("Failed to send message", e);
        }
    }

    @Transactional
    @OperationLog(type = "ORDER_CANCEL", description = "取消订单")
    public void cancel(Long orderId, Long operatorUserId, String operatorRole) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单不存在");
        }
        checkOperatorPermission(operatorUserId, operatorRole, order.getUserId());
        if (!OrderStatus.PENDING_PAY.getValue().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只能取消待支付订单");
        }

        order.setStatus(OrderStatus.CANCELED.getValue());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("Order canceled: orderId={}", orderId);
    }

    public Order getDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单不存在");
        }
        return order;
    }

    public IPage<Order> listByLessorId(Long lessorId, int page, int size) {
        return orderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getLessorId, lessorId)
                        .orderByDesc(Order::getCreatedAt)
        );
    }

    public IPage<Order> listByRenterId(Long userId, int page, int size) {
        return orderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreatedAt)
        );
    }

    public IPage<Order> listByUserId(Long userId, String status, String orderNo, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId);

        // 状态筛选
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }

        // 订单号搜索
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(Order::getOrderNo, orderNo);
        }

        return orderMapper.selectPage(new Page<>(page, size), wrapper.orderByDesc(Order::getCreatedAt));
    }

    private String generateOrderNo() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    public long countOrders() {
        return orderMapper.selectCount(null);
    }

    public IPage<Order> listAllOrders(String status, String orderNo, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(Order::getOrderNo, orderNo);
        }
        return orderMapper.selectPage(new Page<>(page, size), wrapper.orderByDesc(Order::getCreatedAt));
    }

    private void checkOperatorPermission(Long operatorUserId, String operatorRole, Long expectedUserId) {
        if ("ADMIN".equalsIgnoreCase(operatorRole)) {
            return;
        }
        if (operatorUserId == null || !expectedUserId.equals(operatorUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该订单");
        }
    }
}

