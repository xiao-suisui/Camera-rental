package com.example.camerarental.device.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.camerarental.common.annotation.AdminOnly;
import com.example.camerarental.common.result.Result;
import com.example.camerarental.device.dto.DeviceAuditRequest;
import com.example.camerarental.device.dto.DeviceBatchPublishRequest;
import com.example.camerarental.device.dto.DevicePublishRequest;
import com.example.camerarental.device.dto.DevicePublishResponse;
import com.example.camerarental.device.entity.Device;
import com.example.camerarental.device.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/publish")
    public Result<DevicePublishResponse> publish(@Valid @RequestBody DevicePublishRequest request) {
        return Result.success(deviceService.publish(request));
    }

    @PostMapping("/batch-publish")
    public Result<List<DevicePublishResponse>> batchPublish(@Valid @RequestBody DeviceBatchPublishRequest request) {
        return Result.success(deviceService.batchPublish(request));
    }

    @AdminOnly
    @PostMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody DeviceAuditRequest request) {
        deviceService.audit(request);
        return Result.success("audit success", null);
    }

    @GetMapping("/search")
    public Result<IPage<Device>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(deviceService.search(keyword, brand, sortBy, minPrice, maxPrice, page, size));
    }

    @GetMapping("/{deviceId}")
    public Result<Device> getById(@PathVariable Long deviceId) {
        return Result.success(deviceService.getById(deviceId));
    }

    @PutMapping("/{deviceId}")
    public Result<Void> update(@PathVariable Long deviceId, @Valid @RequestBody DevicePublishRequest request) {
        deviceService.updateDevice(deviceId, request);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{deviceId}")
    public Result<Void> delete(@PathVariable Long deviceId) {
        deviceService.deleteDevice(deviceId);
        return Result.success("删除成功", null);
    }

    @PostMapping("/{deviceId}/offline")
    public Result<Void> offline(@PathVariable Long deviceId) {
        deviceService.offlineDevice(deviceId);
        return Result.success("下架成功", null);
    }

    @PostMapping("/{deviceId}/online")
    public Result<Void> online(@PathVariable Long deviceId) {
        deviceService.onlineDevice(deviceId);
        return Result.success("上架成功", null);
    }

    @GetMapping("/my-devices")
    public Result<List<Device>> getMyDevices(@RequestParam Long userId) {
        return Result.success(deviceService.getMyDevices(userId));
    }

    @GetMapping("/stats/count")
    public Result<Long> getDeviceCount() {
        return Result.success(deviceService.countDevices());
    }

    @GetMapping("/stats/pending")
    public Result<Long> getPendingCount() {
        return Result.success(deviceService.countPendingAudit());
    }

    // ========== 管理员接口 ==========

    @AdminOnly
    @GetMapping("/admin/list")
    public Result<IPage<Device>> adminListDevices(
            @RequestParam(required = false) String auditStatus,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(deviceService.adminListDevices(auditStatus, brand, page, size));
    }
}

