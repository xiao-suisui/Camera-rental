package com.example.camerarental.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.camerarental.order.dto.OutboxMetricsResponse;
import com.example.camerarental.order.entity.MqOutboxMessage;
import com.example.camerarental.order.mapper.MqOutboxMessageMapper;
import com.example.camerarental.order.mq.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxMetricsService {

    private final MqOutboxMessageMapper outboxMessageMapper;

    public OutboxMetricsResponse getMetrics() {
        Long pendingCount = outboxMessageMapper.selectCount(
                new LambdaQueryWrapper<MqOutboxMessage>()
                        .eq(MqOutboxMessage::getStatus, OutboxStatus.NEW)
        );

        Long failedCount = outboxMessageMapper.selectCount(
                new LambdaQueryWrapper<MqOutboxMessage>()
                        .eq(MqOutboxMessage::getStatus, OutboxStatus.FAILED)
        );

        QueryWrapper<MqOutboxMessage> retryQuery = new QueryWrapper<>();
        retryQuery.select("COALESCE(SUM(retry_count), 0)");
        List<Object> retryResult = outboxMessageMapper.selectObjs(retryQuery);
        Long retryCount = 0L;
        if (retryResult != null && !retryResult.isEmpty() && retryResult.get(0) != null) {
            retryCount = ((Number) retryResult.get(0)).longValue();
        }

        return new OutboxMetricsResponse(pendingCount, failedCount, retryCount);
    }
}
