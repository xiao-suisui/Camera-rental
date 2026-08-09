package com.example.camerarental.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.camerarental.common.annotation.AdminOnly;
import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.common.result.Result;
import com.example.camerarental.order.dto.DisputeAdminUpdateRequest;
import com.example.camerarental.order.dto.DisputeCreateRequest;
import com.example.camerarental.order.entity.Dispute;
import com.example.camerarental.order.entity.Order;
import com.example.camerarental.order.mapper.OrderMapper;
import com.example.camerarental.order.service.DisputeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dispute")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;
    private final OrderMapper orderMapper;

    /**
     * 创建纠纷（租户或商家均可）
     */
    @PostMapping("/create")
    public Result<Dispute> create(
            @Valid @RequestBody DisputeCreateRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        Dispute dispute = disputeService.createDispute(request, userId);
        return Result.success("纠纷已提交", dispute);
    }

    /**
     * 查询订单的活跃纠纷（聊天界面用）
     */
    @GetMapping("/order/{orderId}")
    public Result<Dispute> getByOrderId(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {
        // 验证调用者是订单参与者
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!userId.equals(order.getUserId()) && !userId.equals(order.getLessorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Dispute dispute = disputeService.getDisputeByOrderId(orderId);
        return Result.success(dispute);
    }

    /**
     * 管理员：纠纷列表
     */
    @AdminOnly
    @GetMapping("/admin/list")
    public Result<IPage<Dispute>> adminList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String disputeType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(disputeService.listDisputes(status, disputeType, page, size));
    }

    /**
     * 管理员：纠纷详情
     */
    @AdminOnly
    @GetMapping("/admin/{disputeId}")
    public Result<Dispute> adminDetail(@PathVariable Long disputeId) {
        return Result.success(disputeService.getDisputeDetail(disputeId));
    }

    /**
     * 管理员：待处理纠纷数量
     */
    @AdminOnly
    @GetMapping("/admin/stats/pending")
    public Result<Long> adminPendingCount() {
        return Result.success(disputeService.countPendingDisputes());
    }

    /**
     * 管理员：更新纠纷状态
     */
    @AdminOnly
    @PutMapping("/admin/{disputeId}")
    public Result<Void> adminUpdate(
            @PathVariable Long disputeId,
            @Valid @RequestBody DisputeAdminUpdateRequest request,
            @RequestHeader("X-User-Id") Long adminId) {
        disputeService.updateDispute(disputeId, request, adminId);
        return Result.success("更新成功", null);
    }
}
