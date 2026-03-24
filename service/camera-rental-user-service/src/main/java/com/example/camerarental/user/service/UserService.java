package com.example.camerarental.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.camerarental.common.annotation.OperationLog;
import com.example.camerarental.common.enums.ErrorCode;
import com.example.camerarental.common.enums.UserStatus;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.user.config.JwtService;
import com.example.camerarental.user.dto.*;
import com.example.camerarental.user.entity.User;
import com.example.camerarental.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public void register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
        }

        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())
        );
        if (existUser != null) {
            throw new BusinessException(ErrorCode.USER_EXISTS, "该手机号已注册");
        }

        User user = User.builder()
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role("USER")
                .status(UserStatus.ACTIVE.getValue())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userMapper.insert(user);
        log.info("User registered: {}", request.getPhone());
    }

    @OperationLog(type = "USER_LOGIN", description = "用户登录", saveResponseData = false)
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())
        );

        if (user == null) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "手机号或密码错误");
        }

        if (UserStatus.DISABLED.getValue().equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "手机号或密码错误");
        }

        String token = jwtService.generateToken(user.getId().toString(), Map.of(
                "role", user.getRole(),
                "nickname", user.getNickname()
        ));

        String refreshToken = jwtService.generateRefreshToken(user.getId());

        log.info("User logged in: {}", request.getPhone());
        return new LoginResponse(token, refreshToken, user.getId(), user.getNickname(), user.getRole());
    }

    @Cacheable(value = "user", key = "#userId")
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Transactional
    @OperationLog(type = "USER_PROFILE_UPDATE", description = "更新个人资料")
    @CacheEvict(value = "user", key = "#userId")
    public void updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户不存在");
        }

        // 检查昵称是否已被使用（排除自己）
        if (!user.getNickname().equals(request.getNickname())) {
            User existUser = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getNickname, request.getNickname())
                            .ne(User::getId, userId)
            );
            if (existUser != null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "昵称已被使用");
            }
        }

        user.setNickname(request.getNickname());
        user.setAddress(request.getAddress());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);
        log.info("User profile updated: userId={}", userId);
    }

    @Transactional
    @OperationLog(type = "USER_PASSWORD_CHANGE", description = "修改密码")
    @CacheEvict(value = "user", key = "#userId")
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "旧密码错误");
        }

        // 新密码不能与旧密码相同
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能与旧密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);
        log.info("User password changed: userId={}", userId);
    }

    public long countUsers() {
        return userMapper.selectCount(null);
    }

    /**
     * 刷新令牌：校验旧 RefreshToken 后签发新的 AccessToken + RefreshToken（旋转刷新）
     */
    public LoginResponse refreshToken(Long userId, String oldRefreshToken) {
        // 1. 校验 RefreshToken 是否合法（存在、未过期、属于该用户）
        if (!jwtService.validateRefreshToken(userId, oldRefreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "RefreshToken 无效或已过期，请重新登录");
        }

        // 2. 校验用户状态
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        if (UserStatus.DISABLED.getValue().equals(user.getStatus())) {
            jwtService.removeRefreshToken(userId);
            throw new BusinessException(ErrorCode.USER_DISABLED, "用户已禁用");
        }

        // 3. 旋转刷新：作废旧 RefreshToken，签发全新的 AccessToken + RefreshToken
        String newAccessToken = jwtService.generateToken(user.getId().toString(), Map.of(
                "role", user.getRole(),
                "nickname", user.getNickname()
        ));
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        log.info("Token refreshed for userId={}", userId);
        return new LoginResponse(newAccessToken, newRefreshToken, user.getId(), user.getNickname(), user.getRole());
    }

    /**
     * 退出登录：清除 RefreshToken
     */
    public void logout(Long userId) {
        jwtService.removeRefreshToken(userId);
        log.info("User logged out: userId={}", userId);
    }

    // ========== 管理员方法 ==========

    public IPage<User> listUsers(int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(User::getPhone, keyword)
                    .or()
                    .like(User::getNickname, keyword)
            );
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return userMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    @OperationLog(type = "USER_STATUS_CHANGE", description = "修改用户状态")
    @CacheEvict(value = "user", key = "#userId")
    public void updateUserStatus(Long userId, String status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能修改管理员状态");
        }
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("User status changed: userId={}, status={}", userId, status);
    }
}

