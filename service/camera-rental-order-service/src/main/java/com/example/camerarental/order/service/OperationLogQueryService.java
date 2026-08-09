package com.example.camerarental.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.camerarental.common.entity.OperationLogEntity;
import com.example.camerarental.common.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogQueryService {

    private final OperationLogMapper operationLogMapper;

    public IPage<OperationLogEntity> pageLogs(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String type,
            String status,
            String urlKeyword,
            Long userId,
            int page,
            int size) {

        LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<>();

        if (startTime != null) {
            wrapper.ge(OperationLogEntity::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLogEntity::getCreatedAt, endTime);
        }
        if (type != null && !type.isBlank()) {
            wrapper.eq(OperationLogEntity::getType, type.trim());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(OperationLogEntity::getStatus, status.trim());
        }
        if (urlKeyword != null && !urlKeyword.isBlank()) {
            wrapper.like(OperationLogEntity::getUrl, urlKeyword.trim());
        }
        if (userId != null) {
            wrapper.eq(OperationLogEntity::getUserId, userId);
        }

        wrapper.orderByDesc(OperationLogEntity::getCreatedAt);

        return operationLogMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
