package com.example.camerarental.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.camerarental.common.annotation.OperationLog;
import com.example.camerarental.common.enums.CouponStatus;
import com.example.camerarental.common.enums.DepositStatus;
import com.example.camerarental.common.enums.DeviceStatus;
import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.enums.OrderStatus;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.common.result.Result;
import com.example.camerarental.order.dto.CouponDTO;
import com.example.camerarental.order.dto.DeviceDTO;
import com.example.camerarental.order.dto.OrderCreateRequest;
import com.example.camerarental.order.dto.OrderCreateResponse;
import com.example.camerarental.order.entity.Order;
import com.example.camerarental.order.feign.CouponFeignClient;
import com.example.camerarental.order.feign.DeviceFeignClient;
import com.example.camerarental.order.mapper.OrderMapper;
import com.example.camerarental.order.mq.OrderNotificationProducer;
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
    private final OrderNotificationProducer orderNotificationProducer;

    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponse createOrder(OrderCreateRequest request, Long operatorUserId, String operatorRole) {
        if (operatorUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未获取到用户身份信息");
        }
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "租赁开始时间不能晚于结束时间");
        }
        if (request.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "租赁结束时间不能早于当前时间");
        }

        // 1. 查询设备信息
        Result<DeviceDTO> deviceResult = deviceFeignClient.getById(request.getDeviceId());
        if (deviceResult == null || deviceResult.getCode() == null || deviceResult.getCode() != 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "设备服务不可用，暂时无法创建订单");
        }
        DeviceDTO device = deviceResult.getData();
        if (device == null) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }
        if (!DeviceStatus.ACTIVE.getValue().equals(device.getStatus())) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_AVAILABLE);
        }
        if (operatorUserId.equals(device.getUserId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能租赁自己发布的设备");
        }

        // 2. 计算租金
        long days = ChronoUnit.DAYS.between(request.getStartTime().toLocalDate(),
                request.getEndTime().toLocalDate()) + 1;
        BigDecimal totalRent = device.getRentPrice().multiply(BigDecimal.valueOf(days));
        BigDecimal discountAmount = BigDecimal.ZERO;

        // 3. 应用优惠券
        if (request.getCouponId() != null) {
            Result<CouponDTO> couponResult = couponFeignClient.getById(request.getCouponId());
            if (couponResult == null || couponResult.getCode() == null || couponResult.getCode() != 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "优惠券服务不可用，暂时无法校验优惠券");
            }
            CouponDTO coupon = couponResult.getData();
            if (coupon == null) {
                throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
            }
            if (!CouponStatus.PUBLISHED.getValue().equals(coupon.getStatus())) {
                throw new BusinessException(ErrorCode.COUPON_EXPIRED, "优惠券不可用或已下架");
            }
            if (coupon.getThreshold() != null && totalRent.compareTo(coupon.getThreshold()) < 0) {
                throw new BusinessException(ErrorCode.COUPON_NOT_MEET_THRESHOLD);
            }
            if (coupon.getDenomination() != null) {
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
        order.setUserId(operatorUserId);
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

        // 通过 Outbox + MQ 异步通知租用者
        try {
            orderNotificationProducer.sendShippedNotification(
                    order.getOrderNo(),
                    order.getUserId(),
                    String.format("您的订单 %s 已发货，物流单号：%s", order.getOrderNo(), trackingNumber)
            );
        } catch (Exception e) {
            log.error("Failed to create shipped notification outbox event", e);
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

        // 通过 Outbox + MQ 异步通知出租者
        try {
            orderNotificationProducer.sendReceivedNotification(
                    order.getOrderNo(),
                    order.getLessorId(),
                    String.format("租用者已确认收货，订单 %s 进入使用中", order.getOrderNo())
            );
        } catch (Exception e) {
            log.error("Failed to create received notification outbox event", e);
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

        // 通过 Outbox + MQ 异步通知出租者
        try {
            orderNotificationProducer.sendReturnAppliedNotification(
                    order.getOrderNo(),
                    order.getLessorId(),
                    String.format("租用者已申请归还订单 %s，请注意查收", order.getOrderNo())
            );
        } catch (Exception e) {
            log.error("Failed to create return-applied notification outbox event", e);
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

        // 通过 Outbox + MQ 异步通知租用者
        try {
            orderNotificationProducer.sendReturnConfirmedNotification(
                    order.getOrderNo(),
                    order.getUserId(),
                    String.format("您的订单 %s 已确认归还，押金已退还", order.getOrderNo())
            );
        } catch (Exception e) {
            log.error("Failed to create return-confirmed notification outbox event", e);
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

        // 通过 Outbox + MQ 发送取消补偿事件（释放设备库存/回滚优惠券）
        orderNotificationProducer.sendOrderCancelledCompensation(
            order.getOrderNo(),
            order.getUserId(),
            order.getDeviceId(),
            order.getCouponId()
        );

        log.info("Order canceled: orderId={}", orderId);
    }

    public Order getDetail(Long orderId, Long operatorUserId, String operatorRole) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单不存在");
        }
        if (!canReadOrder(order, operatorUserId, operatorRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该订单");
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

    private boolean canReadOrder(Order order, Long operatorUserId, String operatorRole) {
        if ("ADMIN".equalsIgnoreCase(operatorRole)) {
            return true;
        }
        if (operatorUserId == null) {
            return false;
        }
        return operatorUserId.equals(order.getUserId()) || operatorUserId.equals(order.getLessorId());
    }
}

