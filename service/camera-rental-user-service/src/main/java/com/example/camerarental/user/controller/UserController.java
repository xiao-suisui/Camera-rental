package com.example.camerarental.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.camerarental.common.annotation.AdminOnly;
import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.enums.UserStatus;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.common.result.Result;
import com.example.camerarental.user.dto.*;
import com.example.camerarental.user.entity.User;
import com.example.camerarental.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success("register success", null);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @GetMapping("/{userId}")
    public Result<User> getUserById(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) String currentUserId,
            @RequestHeader(value = "X-User-Role", required = false) String currentUserRole) {
        if (!"ADMIN".equalsIgnoreCase(currentUserRole)) {
            verifyIdentity(userId, currentUserId);
        }
        return Result.success(userService.getUserById(userId));
    }

    @PutMapping("/{userId}/profile")
    public Result<Void> updateProfile(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) String currentUserId,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        verifyIdentity(userId, currentUserId);
        userService.updateProfile(userId, request);
        return Result.success("更新成功", null);
    }

    @PostMapping("/{userId}/change-password")
    public Result<Void> changePassword(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) String currentUserId,
            @Valid @RequestBody ChangePasswordRequest request) {
        verifyIdentity(userId, currentUserId);
        userService.changePassword(userId, request);
        return Result.success("密码修改成功", null);
    }

    @GetMapping("/stats/count")
    public Result<Long> getUserCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(userService.countUsers());
    }

    /**
     * 刷新令牌（双令牌机制）：客户端提交 userId + 旧 RefreshToken，后端校验后签发全新 AccessToken + RefreshToken
     * 该接口已加入网关白名单，不需要有效的 AccessToken
     */
    @PostMapping("/refresh-token")
    public Result<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(userService.refreshToken(request.getUserId(), request.getRefreshToken()));
    }

    /**
     * 退出登录：清除服务端 RefreshToken
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        if (userIdStr != null && !userIdStr.isEmpty()) {
            userService.logout(Long.parseLong(userIdStr));
        }
        return Result.success("退出成功", null);
    }

    // ========== 管理员接口 ==========

    @AdminOnly
    @GetMapping("/admin/list")
    public Result<IPage<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.listUsers(page, size, keyword));
    }

    @AdminOnly
    @PutMapping("/admin/{userId}/disable")
    public Result<Void> disableUser(@PathVariable Long userId) {
        userService.updateUserStatus(userId, UserStatus.DISABLED.getValue());
        return Result.success("用户已禁用", null);
    }

    @AdminOnly
    @PutMapping("/admin/{userId}/enable")
    public Result<Void> enableUser(@PathVariable Long userId) {
        userService.updateUserStatus(userId, UserStatus.ACTIVE.getValue());
        return Result.success("用户已启用", null);
    }

    /**
     * 验证当前登录用户与操作目标用户一致（防止 IDOR 越权）
     */
    private void verifyIdentity(Long targetUserId, String currentUserIdStr) {
        if (currentUserIdStr == null || currentUserIdStr.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未获取到用户身份信息");
        }
        try {
            Long currentUserId = Long.parseLong(currentUserIdStr);
            if (!targetUserId.equals(currentUserId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作其他用户的数据");
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户身份信息无效");
        }
    }
}

