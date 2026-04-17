package com.example.camerarental.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.camerarental.common.annotation.OperationLog;
import com.example.camerarental.common.enums.AuditStatus;
import com.example.camerarental.common.enums.DeviceStatus;
import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.common.mq.OrderCancelledEvent;
import com.example.camerarental.device.dto.DeviceAuditRequest;
import com.example.camerarental.device.dto.DeviceBatchPublishRequest;
import com.example.camerarental.device.dto.DevicePublishRequest;
import com.example.camerarental.device.dto.DevicePublishResponse;
import com.example.camerarental.device.entity.Device;
import com.example.camerarental.device.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceMapper deviceMapper;

    @Transactional
    @OperationLog(type = "DEVICE_PUBLISH", description = "发布设备")
    public DevicePublishResponse publish(DevicePublishRequest request) {
        Device device = Device.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .brand(request.getBrand())
                .model(request.getModel())
                .description(request.getDescription())
                .imagePaths(String.join(",", request.getImagePaths()))
                .rentPrice(request.getRentPrice())
                .deposit(request.getDeposit())
                .address(request.getAddress())
                .auditStatus(AuditStatus.PENDING.getValue())
                .status(DeviceStatus.OFFLINE.getValue())
                .deleted(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        deviceMapper.insert(device);
        log.info("Device published: deviceId={}", device.getId());
        return new DevicePublishResponse(device.getId(), device.getAuditStatus());
    }

    @Transactional
    @OperationLog(type = "DEVICE_BATCH_PUBLISH", description = "批量发布设备")
    public List<DevicePublishResponse> batchPublish(DeviceBatchPublishRequest request) {
        String batchId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        List<Device> devices = new ArrayList<>(request.getQuantity());

        for (int i = 0; i < request.getQuantity(); i++) {
            devices.add(Device.builder()
                    .userId(request.getUserId())
                    .title(request.getTitle() + (request.getQuantity() > 1 ? " #" + (i + 1) : ""))
                    .brand(request.getBrand())
                    .model(request.getModel())
                    .description(request.getDescription())
                    .imagePaths(String.join(",", request.getImagePaths()))
                    .rentPrice(request.getRentPrice())
                    .deposit(request.getDeposit())
                    .address(request.getAddress())
                    .auditStatus(AuditStatus.PENDING.getValue())
                    .status(DeviceStatus.OFFLINE.getValue())
                    .batchId(batchId)
                    .deleted(0)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }

        int affectedRows = deviceMapper.insertBatch(devices);
        if (affectedRows != request.getQuantity()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量发布失败，请重试");
        }

        List<DevicePublishResponse> responses = deviceMapper.selectList(
                        new LambdaQueryWrapper<Device>()
                                .eq(Device::getBatchId, batchId)
                                .eq(Device::getUserId, request.getUserId())
                                .orderByAsc(Device::getId)
                ).stream()
                .map(device -> new DevicePublishResponse(device.getId(), device.getAuditStatus()))
                .collect(Collectors.toList());

        log.info("Batch devices published: batchId={}, count={}", batchId, request.getQuantity());
        return responses;
    }

    @Transactional
    @OperationLog(type = "DEVICE_AUDIT", description = "审核设备")
    @CacheEvict(value = "device", key = "#request.deviceId")
    public void audit(DeviceAuditRequest request) {
        Device device = deviceMapper.selectById(request.getDeviceId());
        if (device == null) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }

        device.setAuditStatus(request.getAuditStatus());
        if (AuditStatus.APPROVED.getValue().equals(request.getAuditStatus())) {
            device.setStatus(DeviceStatus.ACTIVE.getValue());
        } else if (AuditStatus.REJECTED.getValue().equals(request.getAuditStatus())) {
            device.setStatus(DeviceStatus.OFFLINE.getValue());
        }
        device.setUpdatedAt(LocalDateTime.now());

        deviceMapper.updateById(device);
        log.info("Device audited: deviceId={}, status={}", device.getId(), request.getAuditStatus());
    }

    public IPage<Device> search(String keyword, String brand, String sortBy,
                                BigDecimal minPrice, BigDecimal maxPrice,
                                int page, int size) {
        Page<Device> pageRequest = new Page<>(page, size);

        // 根据sortBy参数设置排序
        if (sortBy != null && !sortBy.isEmpty()) {
            switch (sortBy) {
                case "price_asc":
                    pageRequest.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.asc("rent_price"));
                    break;
                case "price_desc":
                    pageRequest.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.desc("rent_price"));
                    break;
                case "time_desc":
                    pageRequest.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.desc("created_at"));
                    break;
                default:
                    pageRequest.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.desc("created_at"));
            }
        } else {
            // 默认按创建时间倒序
            pageRequest.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.desc("created_at"));
        }

        return deviceMapper.searchDevices(pageRequest, keyword, brand, minPrice, maxPrice);
    }

    @Cacheable(value = "device", key = "#deviceId")
    public Device getById(Long deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }
        return device;
    }

    @Transactional
    @OperationLog(type = "DEVICE_UPDATE", description = "更新设备")
    @CacheEvict(value = "device", key = "#deviceId")
    public void updateDevice(Long deviceId, DevicePublishRequest request) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }

        device.setTitle(request.getTitle());
        device.setBrand(request.getBrand());
        device.setModel(request.getModel());
        device.setDescription(request.getDescription());
        device.setImagePaths(String.join(",", request.getImagePaths()));
        device.setRentPrice(request.getRentPrice());
        device.setDeposit(request.getDeposit());
        device.setAddress(request.getAddress());
        device.setUpdatedAt(LocalDateTime.now());

        deviceMapper.updateById(device);
        log.info("Device updated: deviceId={}", deviceId);
    }

    @Transactional
    @OperationLog(type = "DEVICE_DELETE", description = "删除设备")
    @CacheEvict(value = "device", key = "#deviceId")
    public void deleteDevice(Long deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }

        device.setDeleted(1);
        device.setUpdatedAt(LocalDateTime.now());
        deviceMapper.updateById(device);
        log.info("Device deleted: deviceId={}", deviceId);
    }

    @Transactional
    @OperationLog(type = "DEVICE_OFFLINE", description = "下架设备")
    @CacheEvict(value = "device", key = "#deviceId")
    public void offlineDevice(Long deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }

        device.setStatus(DeviceStatus.OFFLINE.getValue());
        device.setUpdatedAt(LocalDateTime.now());
        deviceMapper.updateById(device);
        log.info("Device offline: deviceId={}", deviceId);
    }

    @Transactional
    @OperationLog(type = "DEVICE_ONLINE", description = "上架设备")
    @CacheEvict(value = "device", key = "#deviceId")
    public void onlineDevice(Long deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }

        device.setStatus(DeviceStatus.ACTIVE.getValue());
        device.setUpdatedAt(LocalDateTime.now());
        deviceMapper.updateById(device);
        log.info("Device online: deviceId={}", deviceId);
    }

    public List<Device> getMyDevices(Long userId) {
        return deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getUserId, userId)
                        .eq(Device::getDeleted, 0)
                        .orderByDesc(Device::getCreatedAt)
        );
    }

    public long countDevices() {
        return deviceMapper.selectCount(
                new LambdaQueryWrapper<Device>().eq(Device::getDeleted, 0)
        );
    }

    public long countPendingAudit() {
        return deviceMapper.selectCount(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeleted, 0)
                        .eq(Device::getAuditStatus, AuditStatus.PENDING.getValue())
        );
    }

    public IPage<Device> adminListDevices(String auditStatus, String brand, int page, int size) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
                .eq(Device::getDeleted, 0);
        if (auditStatus != null && !auditStatus.isEmpty()) {
            wrapper.eq(Device::getAuditStatus, auditStatus);
        }
        if (brand != null && !brand.isEmpty()) {
            wrapper.eq(Device::getBrand, brand);
        }
        wrapper.orderByDesc(Device::getCreatedAt);
        return deviceMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public void handleOrderCancelledReleaseStock(OrderCancelledEvent event) {
        if (event == null || event.getDeviceId() == null) {
            log.warn("Skip stock release for invalid order-cancelled event: {}", event);
            return;
        }

        Device device = deviceMapper.selectById(event.getDeviceId());
        if (device == null || Integer.valueOf(1).equals(device.getDeleted())) {
            log.warn("Skip stock release because device not found or deleted: deviceId={}", event.getDeviceId());
            return;
        }

        device.setStatus(DeviceStatus.ACTIVE.getValue());
        device.setUpdatedAt(LocalDateTime.now());
        deviceMapper.updateById(device);
        log.info("Order-cancelled stock released: orderNo={}, deviceId={}", event.getOrderNo(), event.getDeviceId());
    }
}

