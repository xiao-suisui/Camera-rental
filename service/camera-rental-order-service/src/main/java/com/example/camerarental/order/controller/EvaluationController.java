package com.example.camerarental.order.controller;

import com.example.camerarental.common.annotation.AdminOnly;
import com.example.camerarental.common.result.Result;
import com.example.camerarental.order.dto.EvaluationCreateRequest;
import com.example.camerarental.order.entity.Evaluation;
import com.example.camerarental.order.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/evaluation")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    /**
     * 创建评价
     */
    @PostMapping("/create")
    public Result<Void> create(
            @Valid @RequestBody EvaluationCreateRequest request,
            @RequestHeader("X-User-Id") Long operatorUserId,
            @RequestHeader(value = "X-User-Role", required = false) String operatorRole) {
        evaluationService.createEvaluation(request, operatorUserId, operatorRole);
        return Result.success("评价成功", null);
    }

    /**
     * 查看设备的评价列表
     */
    @GetMapping("/device/{deviceId}")
    public Result<List<Evaluation>> listByDevice(@PathVariable Long deviceId) {
        return Result.success(evaluationService.listByDeviceId(deviceId));
    }

    /**
     * 查看我收到的评价
     */
    @GetMapping("/user/me")
    public Result<List<Evaluation>> listMyEvaluations(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(evaluationService.listByEvaluatedId(userId));
    }

    /**
     * 查看指定用户收到的评价（仅管理员）
     */
    @AdminOnly
    @GetMapping("/user/{userId}")
    public Result<List<Evaluation>> listByUser(@PathVariable Long userId) {
        return Result.success(evaluationService.listByEvaluatedId(userId));
    }

    /**
     * 查看订单的评价
     */
    @GetMapping("/order/{orderId}")
    public Result<List<Evaluation>> listByOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long operatorUserId,
            @RequestHeader(value = "X-User-Role", required = false) String operatorRole) {
        return Result.success(evaluationService.listByOrderId(orderId, operatorUserId, operatorRole));
    }

    /**
     * 删除评价
     */
    @DeleteMapping("/{evaluationId}")
    public Result<Void> delete(
            @PathVariable Long evaluationId,
            @RequestHeader("X-User-Id") Long userId) {
        evaluationService.deleteEvaluation(evaluationId, userId);
        return Result.success("删除成功", null);
    }

    /**
     * 检查是否可以评价
     */
    @GetMapping("/can-evaluate")
    public Result<Boolean> canEvaluate(
            @RequestParam Long orderId,
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(evaluationService.canEvaluate(orderId, userId));
    }
}

