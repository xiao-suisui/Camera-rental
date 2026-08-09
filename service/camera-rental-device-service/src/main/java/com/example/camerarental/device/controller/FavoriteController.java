package com.example.camerarental.device.controller;

import com.example.camerarental.common.result.Result;
import com.example.camerarental.device.entity.Device;
import com.example.camerarental.device.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 收藏设备
     */
    @PostMapping("/add")
    public Result<Void> addFavorite(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long deviceId) {
        favoriteService.addFavorite(userId, deviceId);
        return Result.success("收藏成功", null);
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/remove")
    public Result<Void> removeFavorite(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long deviceId) {
        favoriteService.removeFavorite(userId, deviceId);
        return Result.success("取消收藏成功", null);
    }

    /**
     * 检查是否已收藏
     */
    @GetMapping("/check")
    public Result<Boolean> checkFavorite(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long deviceId) {
        return Result.success(favoriteService.isFavorited(userId, deviceId));
    }

    /**
     * 获取用户收藏列表
     */
    @GetMapping("/list")
    public Result<List<Device>> listFavorites(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(favoriteService.listFavoriteDevices(userId));
    }

    /**
     * 获取收藏数量
     */
    @GetMapping("/count")
    public Result<Integer> getFavoriteCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(favoriteService.getFavoriteCount(userId));
    }
}

