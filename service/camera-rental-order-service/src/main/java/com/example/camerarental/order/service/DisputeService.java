package com.example.camerarental.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.enums.OrderStatus;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.order.dto.DisputeAdminUpdateRequest;
import com.example.camerarental.order.dto.DisputeCreateRequest;
import com.example.camerarental.order.entity.Dispute;
import com.example.camerarental.order.entity.Order;
import com.example.camerarental.order.mapper.DisputeMapper;
import com.example.camerarental.order.mapper.OrderMapper;
import com.example.camerarental.order.mq.OrderNotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeMapper disputeMapper;
    private final OrderMapper orderMapper;
    private final OrderNotificationProducer orderNotificationProducer;

    private static final Set<String> ALLOWED_ORDER_STATUSES = Set.of(
            OrderStatus.PAID.name(),
            OrderStatus.TO_SHIP.name(),
            OrderStatus.TO_RETURN.name()
    );

    private static final Set<String> TERMINAL_STATUSES = Set.of("RESOLVED", "REJECTED");

    @Transactional
    public Dispute createDispute(DisputeCreateRequest request, Long initiatorId) {
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        if (!ALLOWED_ORDER_STATUSES.contains(order.getStatus())) {
            throw new BusinessException(ErrorCode.DISPUTE_ORDER_STATUS_INVALID);
        }

        if (request.getDescription().length() < 10) {
            throw new BusinessException(ErrorCode.DISPUTE_DESCRIPTION_TOO_SHORT);
        }

        // 检查是否已有进行中的纠纷
        Long activeCount = disputeMapper.selectCount(
                new LambdaQueryWrapper<Dispute>()
                        .eq(Dispute::getOrderId, order.getId())
                        .notIn(Dispute::getStatus, TERMINAL_STATUSES)
        );
        if (activeCount > 0) {
            throw new BusinessException(ErrorCode.DISPUTE_ALREADY_EXISTS);
        }

        // 确定被申请人
        Long respondentId = initiatorId.equals(order.getUserId())
                ? order.getLessorId() : order.getUserId();

        Dispute dispute = Dispute.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .initiatorId(initiatorId)
                .respondentId(respondentId)
                .deviceId(order.getDeviceId())
                .disputeType(request.getDisputeType())
                .description(request.getDescription())
                .status("SUBMITTED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        disputeMapper.insert(dispute);

        // 通知管理员（通过 MQ Outbox）
        String typeLabel = getDisputeTypeLabel(request.getDisputeType());
        orderNotificationProducer.sendDisputeCreatedNotifyAdmins(
                order.getOrderNo(),
                String.format("新纠纷工单：订单 %s，类型：%s，请及时处理", order.getOrderNo(), typeLabel)
        );

        log.info("Dispute created: disputeId={}, orderId={}", dispute.getId(), order.getId());
        return dispute;
    }

    public Dispute getDisputeByOrderId(Long orderId) {
        return disputeMapper.selectOne(
                new LambdaQueryWrapper<Dispute>()
                        .eq(Dispute::getOrderId, orderId)
                        .notIn(Dispute::getStatus, TERMINAL_STATUSES)
                        .last("LIMIT 1")
        );
    }

    public Dispute getDisputeDetail(Long disputeId) {
        Dispute dispute = disputeMapper.selectById(disputeId);
        if (dispute == null) {
            throw new BusinessException(ErrorCode.DISPUTE_NOT_FOUND);
        }
        return dispute;
    }

    public IPage<Dispute> listDisputes(String status, String disputeType, int page, int size) {
        LambdaQueryWrapper<Dispute> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Dispute::getStatus, status);
        }
        if (disputeType != null && !disputeType.isEmpty()) {
            wrapper.eq(Dispute::getDisputeType, disputeType);
        }
        wrapper.orderByDesc(Dispute::getCreatedAt);
        return disputeMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public void updateDispute(Long disputeId, DisputeAdminUpdateRequest request, Long adminId) {
        Dispute dispute = disputeMapper.selectById(disputeId);
        if (dispute == null) {
            throw new BusinessException(ErrorCode.DISPUTE_NOT_FOUND);
        }

        String currentStatus = dispute.getStatus();
        String newStatus = request.getStatus();

        // 状态流转校验
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new BusinessException(ErrorCode.DISPUTE_STATUS_ERROR,
                    String.format("不能从 %s 变更为 %s", currentStatus, newStatus));
        }

        dispute.setStatus(newStatus);
        dispute.setAdminId(adminId);
        dispute.setAdminRemark(request.getAdminRemark());
        dispute.setUpdatedAt(LocalDateTime.now());
        disputeMapper.updateById(dispute);

        // 终态时通知双方（通过 MQ Outbox）
        if (TERMINAL_STATUSES.contains(newStatus)) {
            String result = "RESOLVED".equals(newStatus) ? "已解决" : "已驳回";
            String msg = String.format("您的纠纷工单（订单 %s）%s%s",
                    dispute.getOrderNo(), result,
                    request.getAdminRemark() != null ? "，处理意见：" + request.getAdminRemark() : "");
            orderNotificationProducer.sendDisputeResolvedNotifyUser(dispute.getOrderNo(), dispute.getInitiatorId(), msg);
            orderNotificationProducer.sendDisputeResolvedNotifyUser(dispute.getOrderNo(), dispute.getRespondentId(), msg);
        }

        log.info("Dispute updated: disputeId={}, status={}", disputeId, newStatus);
    }

    public long countPendingDisputes() {
        return disputeMapper.selectCount(
                new LambdaQueryWrapper<Dispute>()
                        .in(Dispute::getStatus, "SUBMITTED", "PROCESSING")
        );
    }

    private boolean isValidTransition(String current, String next) {
        return switch (current) {
            case "SUBMITTED" -> "PROCESSING".equals(next);
            case "PROCESSING" -> "RESOLVED".equals(next) || "REJECTED".equals(next);
            default -> false;
        };
    }

    private String getDisputeTypeLabel(String type) {
        return switch (type) {
            case "DEVICE_DAMAGE" -> "设备损坏";
            case "DEPOSIT_DISPUTE" -> "押金纠纷";
            case "OVERDUE" -> "逾期纠纷";
            case "DESCRIPTION_MISMATCH" -> "描述不符";
            default -> "其他";
        };
    }
}
