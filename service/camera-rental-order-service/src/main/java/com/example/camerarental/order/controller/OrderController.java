package com.example.camerarental.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.camerarental.common.annotation.AdminOnly;
import com.example.camerarental.common.entity.OperationLogEntity;
import com.example.camerarental.common.annotation.RepeatSubmit;
import com.example.camerarental.common.result.Result;
import com.example.camerarental.order.dto.OrderCreateRequest;
import com.example.camerarental.order.dto.OrderCreateResponse;
import com.example.camerarental.order.dto.OutboxMetricsResponse;
import com.example.camerarental.order.dto.OrderReturnApplyRequest;
import com.example.camerarental.order.dto.OrderReturnConfirmRequest;
import com.example.camerarental.order.dto.OrderShipRequest;
import com.example.camerarental.order.entity.Order;
import com.example.camerarental.order.service.OperationLogQueryService;
import com.example.camerarental.order.service.OutboxMetricsService;
import com.example.camerarental.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OutboxMetricsService outboxMetricsService;
    private final OperationLogQueryService operationLogQueryService;

    @PostMapping("/create")
    @RepeatSubmit(interval = 5, message = "订单正在创建中，请勿重复提交")
    public Result<OrderCreateResponse> create(
            @Valid @RequestBody OrderCreateRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return Result.success(orderService.createOrder(request, userId, role));
    }

    @PostMapping("/{orderId}/pay")
    public Result<Void> mockPay(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        orderService.mockPay(orderId, userId, role);
        return Result.success("支付成功", null);
    }

    @PostMapping("/{orderId}/ship")
    public Result<Void> ship(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderShipRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        orderService.ship(orderId, request.getTrackingNumber(), userId, role);
        return Result.success("发货成功", null);
    }

    @PostMapping("/{orderId}/receive")
    public Result<Void> receive(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        orderService.receive(orderId, userId, role);
        return Result.success("确认收货成功", null);
    }

    @PostMapping("/{orderId}/return")
    public Result<Void> applyReturn(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderReturnApplyRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        orderService.applyReturn(orderId, request.getTrackingNumber(), userId, role);
        return Result.success("归还申请已提交", null);
    }

    @PostMapping("/{orderId}/confirm")
    public Result<Void> confirmReturn(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderReturnConfirmRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        orderService.confirmReturn(orderId, request.getCondition(), userId, role);
        return Result.success("归还确认成功", null);
    }

    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancel(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        orderService.cancel(orderId, userId, role);
        return Result.success("订单已取消", null);
    }

    @GetMapping("/{orderId}")
    public Result<Order> getDetail(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return Result.success(orderService.getDetail(orderId, userId, role));
    }

    @GetMapping("/list")
    public Result<IPage<Order>> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(orderService.listByUserId(userId, status, orderNo, page, size));
    }

    @GetMapping("/lessor/list")
    public Result<IPage<Order>> listLessorOrders(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(orderService.listByLessorId(userId, page, size));
    }

    @GetMapping("/renter/list")
    public Result<IPage<Order>> listRenterOrders(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(orderService.listByRenterId(userId, page, size));
    }

    @GetMapping("/stats/count")
    public Result<Long> getOrderCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.countOrders());
    }

    // ========== 管理员接口 ==========

    @AdminOnly
    @GetMapping("/admin/list")
    public Result<IPage<Order>> listAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(orderService.listAllOrders(status, orderNo, page, size));
    }

    @AdminOnly
    @GetMapping("/admin/outbox/metrics")
    public Result<OutboxMetricsResponse> getOutboxMetrics() {
        return Result.success(outboxMetricsService.getMetrics());
    }

    @AdminOnly
    @GetMapping("/admin/operation-logs")
    public Result<IPage<OperationLogEntity>> listOperationLogs(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String urlKeyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(operationLogQueryService.pageLogs(startTime, endTime, type, status, urlKeyword, userId, page, size));
    }
}

