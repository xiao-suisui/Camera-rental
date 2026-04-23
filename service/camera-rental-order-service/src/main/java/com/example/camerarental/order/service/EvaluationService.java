package com.example.camerarental.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.camerarental.common.annotation.OperationLog;
import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.enums.OrderStatus;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.common.service.MinioService;
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
    private final MinioService minioService;

    /**
     * 创建评价
     */
    @Transactional
    @OperationLog(type = "EVALUATION_CREATE", description = "创建评价")
    public void createEvaluation(EvaluationCreateRequest request, Long operatorUserId, String operatorRole) {
        // 检查订单是否存在
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单不存在");
        }

        // 非管理员必须是订单参与方，且评价者必须是当前请求用户。
        if (!"ADMIN".equalsIgnoreCase(operatorRole)) {
            boolean participant = operatorUserId != null
                    && (operatorUserId.equals(order.getUserId()) || operatorUserId.equals(order.getLessorId()));
            if (!participant) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权评价该订单");
            }
            if (request.getEvaluatorId() != null && !operatorUserId.equals(request.getEvaluatorId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "评价者身份不一致");
            }
        }

        // 检查订单状态（只有已完成的订单才能评价）
        if (!OrderStatus.FINISHED.getValue().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单未完成，无法评价");
        }

        Long evaluatorId = operatorUserId != null ? operatorUserId : request.getEvaluatorId();
        if (evaluatorId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "缺少评价者信息");
        }

        // 计算被评价者与设备，避免信任前端可篡改字段
        Long evaluatedId = evaluatorId.equals(order.getUserId()) ? order.getLessorId() : order.getUserId();
        Long deviceId = order.getDeviceId();

        if (request.getEvaluatedId() != null && !request.getEvaluatedId().equals(evaluatedId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "被评价者信息不一致");
        }
        if (request.getDeviceId() != null && !request.getDeviceId().equals(deviceId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "设备信息不一致");
        }

        // 检查是否已经评价过
        Evaluation existing = evaluationMapper.selectOne(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getOrderId, request.getOrderId())
                        .eq(Evaluation::getEvaluatorId, evaluatorId)
        );

        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已经评价过该订单");
        }

        // 创建评价
        Evaluation evaluation = Evaluation.builder()
                .orderId(request.getOrderId())
            .evaluatorId(evaluatorId)
            .evaluatedId(evaluatedId)
            .deviceId(deviceId)
                .score(request.getScore())
                .content(request.getContent())
                .imagePaths(minioService.normalizeImagePaths(request.getImagePaths()))
                .createdAt(LocalDateTime.now())
                .build();

        evaluationMapper.insert(evaluation);
        log.info("Evaluation created: orderId={}, evaluatorId={}", request.getOrderId(), evaluatorId);
    }

    /**
     * 查看设备的评价列表
     */
    public List<Evaluation> listByDeviceId(Long deviceId) {
        List<Evaluation> evaluations = evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getDeviceId, deviceId)
                        .orderByDesc(Evaluation::getCreatedAt)
        );
        evaluations.forEach(this::resolveImagePathsForRead);
        return evaluations;
    }

    /**
     * 查看用户收到的评价
     */
    public List<Evaluation> listByEvaluatedId(Long evaluatedId) {
        List<Evaluation> evaluations = evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getEvaluatedId, evaluatedId)
                        .orderByDesc(Evaluation::getCreatedAt)
        );
        evaluations.forEach(this::resolveImagePathsForRead);
        return evaluations;
    }

    public List<Evaluation> listByEvaluatedId(Long evaluatedId, Long operatorUserId, String operatorRole) {
        if (!"ADMIN".equalsIgnoreCase(operatorRole)
                && (operatorUserId == null || !operatorUserId.equals(evaluatedId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该用户评价");
        }
        return listByEvaluatedId(evaluatedId);
    }

    /**
     * 查看订单的评价
     */
    public List<Evaluation> listByOrderId(Long orderId, Long operatorUserId, String operatorRole) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单不存在");
        }

        if (!"ADMIN".equalsIgnoreCase(operatorRole)) {
            boolean allowed = operatorUserId != null
                    && (operatorUserId.equals(order.getUserId()) || operatorUserId.equals(order.getLessorId()));
            if (!allowed) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该订单评价");
            }
        }

        List<Evaluation> evaluations = evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getOrderId, orderId)
                        .orderByDesc(Evaluation::getCreatedAt)
        );
        evaluations.forEach(this::resolveImagePathsForRead);
        return evaluations;
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

        // 仅订单相关方可评价
        if (!userId.equals(order.getUserId()) && !userId.equals(order.getLessorId())) {
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

    private void resolveImagePathsForRead(Evaluation evaluation) {
        if (evaluation == null || evaluation.getImagePaths() == null || evaluation.getImagePaths().isBlank()) {
            return;
        }
        evaluation.setImagePaths(minioService.toSignedUrlPaths(evaluation.getImagePaths()));
    }
}

