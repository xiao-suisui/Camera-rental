package com.example.camerarental.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.camerarental.common.annotation.OperationLog;
import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.enums.OrderStatus;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.order.dto.EvaluationCreateRequest;
import com.example.camerarental.order.entity.Evaluation;
import com.example.camerarental.order.entity.Order;
import com.example.camerarental.order.mapper.EvaluationMapper;
import com.example.camerarental.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationMapper evaluationMapper;
    private final OrderMapper orderMapper;

    /**
     * 创建评价
     */
    @Transactional
    @OperationLog(type = "EVALUATION_CREATE", description = "创建评价")
    public void createEvaluation(EvaluationCreateRequest request) {
        // 检查订单是否存在
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单不存在");
        }

        // 检查订单状态（只有已完成的订单才能评价）
        if (!OrderStatus.FINISHED.getValue().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单未完成，无法评价");
        }

        // 检查是否已经评价过
        Evaluation existing = evaluationMapper.selectOne(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getOrderId, request.getOrderId())
                        .eq(Evaluation::getEvaluatorId, request.getEvaluatorId())
        );

        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已经评价过该订单");
        }

        // 创建评价
        Evaluation evaluation = Evaluation.builder()
                .orderId(request.getOrderId())
                .evaluatorId(request.getEvaluatorId())
                .evaluatedId(request.getEvaluatedId())
                .deviceId(request.getDeviceId())
                .score(request.getScore())
                .content(request.getContent())
                .imagePaths(request.getImagePaths())
                .createdAt(LocalDateTime.now())
                .build();

        evaluationMapper.insert(evaluation);
        log.info("Evaluation created: orderId={}, evaluatorId={}", request.getOrderId(), request.getEvaluatorId());
    }

    /**
     * 查看设备的评价列表
     */
    public List<Evaluation> listByDeviceId(Long deviceId) {
        return evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getDeviceId, deviceId)
                        .orderByDesc(Evaluation::getCreatedAt)
        );
    }

    /**
     * 查看用户收到的评价
     */
    public List<Evaluation> listByEvaluatedId(Long evaluatedId) {
        return evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getEvaluatedId, evaluatedId)
                        .orderByDesc(Evaluation::getCreatedAt)
        );
    }

    /**
     * 查看订单的评价
     */
    public List<Evaluation> listByOrderId(Long orderId) {
        return evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getOrderId, orderId)
                        .orderByDesc(Evaluation::getCreatedAt)
        );
    }

    /**
     * 删除评价
     */
    @Transactional
    @OperationLog(type = "EVALUATION_DELETE", description = "删除评价")
    public void deleteEvaluation(Long evaluationId, Long userId) {
        Evaluation evaluation = evaluationMapper.selectById(evaluationId);
        if (evaluation == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评价不存在");
        }

        // 只能删除自己的评价
        if (!evaluation.getEvaluatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只能删除自己的评价");
        }

        evaluationMapper.deleteById(evaluationId);
        log.info("Evaluation deleted: id={}, userId={}", evaluationId, userId);
    }

    /**
     * 检查用户是否可以评价该订单
     */
    public boolean canEvaluate(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !OrderStatus.FINISHED.getValue().equals(order.getStatus())) {
            return false;
        }

        // 检查是否已评价
        long count = evaluationMapper.selectCount(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getOrderId, orderId)
                        .eq(Evaluation::getEvaluatorId, userId)
        );

        return count == 0;
    }
}

