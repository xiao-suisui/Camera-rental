package com.example.camerarental.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.camerarental.common.annotation.OperationLog;
import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.device.entity.Device;
import com.example.camerarental.device.entity.DeviceFavorite;
import com.example.camerarental.device.mapper.DeviceFavoriteMapper;
import com.example.camerarental.device.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private static final int MAX_FAVORITE_COUNT = 50; // 最多收藏50个
    private final DeviceFavoriteMapper favoriteMapper;
    private final DeviceMapper deviceMapper;

    /**
     * 收藏设备
     */
    @Transactional
    @OperationLog(type = "DEVICE_FAVORITE", description = "收藏设备")
    public void addFavorite(Long userId, Long deviceId) {
        // 检查设备是否存在
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "设备不存在");
        }

        // 检查是否已收藏
        DeviceFavorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<DeviceFavorite>()
                        .eq(DeviceFavorite::getUserId, userId)
                        .eq(DeviceFavorite::getDeviceId, deviceId)
        );

        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已经收藏过该设备");
        }

        // 检查收藏数量限制
        long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<DeviceFavorite>()
                        .eq(DeviceFavorite::getUserId, userId)
        );

        if (count >= MAX_FAVORITE_COUNT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    String.format("收藏数量已达上限（%d个）", MAX_FAVORITE_COUNT));
        }

        // 创建收藏记录
        DeviceFavorite favorite = DeviceFavorite.builder()
                .userId(userId)
                .deviceId(deviceId)
                .createdAt(LocalDateTime.now())
                .build();

        favoriteMapper.insert(favorite);
        log.info("User {} favorited device {}", userId, deviceId);
    }

    /**
     * 取消收藏
     */
    @Transactional
    @OperationLog(type = "DEVICE_UNFAVORITE", description = "取消收藏")
    public void removeFavorite(Long userId, Long deviceId) {
        int deleted = favoriteMapper.delete(
                new LambdaQueryWrapper<DeviceFavorite>()
                        .eq(DeviceFavorite::getUserId, userId)
                        .eq(DeviceFavorite::getDeviceId, deviceId)
        );

        if (deleted == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "收藏记录不存在");
        }

        log.info("User {} unfavorited device {}", userId, deviceId);
    }

    /**
     * 检查是否已收藏
     */
    public boolean isFavorited(Long userId, Long deviceId) {
        long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<DeviceFavorite>()
                        .eq(DeviceFavorite::getUserId, userId)
                        .eq(DeviceFavorite::getDeviceId, deviceId)
        );
        return count > 0;
    }

    /**
     * 获取用户收藏的设备列表
     */
    public List<Device> listFavoriteDevices(Long userId) {
        // 查询收藏记录
        List<DeviceFavorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<DeviceFavorite>()
                        .eq(DeviceFavorite::getUserId, userId)
                        .orderByDesc(DeviceFavorite::getCreatedAt)
        );

        if (favorites.isEmpty()) {
            return List.of();
        }

        // 查询设备详情
        List<Long> deviceIds = favorites.stream()
                .map(DeviceFavorite::getDeviceId)
                .collect(Collectors.toList());

        return deviceMapper.selectBatchIds(deviceIds);
    }

    /**
     * 获取用户收藏数量
     */
    public int getFavoriteCount(Long userId) {
        return Math.toIntExact(favoriteMapper.selectCount(
                new LambdaQueryWrapper<DeviceFavorite>()
                        .eq(DeviceFavorite::getUserId, userId)
        ));
    }
}

