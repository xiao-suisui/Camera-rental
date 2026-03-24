package com.example.camerarental.common.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 参数校验工具类
 */
@Slf4j
public class ValidationUtils {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    /**
     * 校验手机号
     */
    public static boolean isValidPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 校验邮箱
     */
    public static boolean isValidEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 校验密码强度（至少6位，包含数字和字母）
     */
    public static boolean isStrongPassword(String password) {
        if (StrUtil.isBlank(password) || password.length() < 6) {
            return false;
        }
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        return hasDigit && hasLetter;
    }

    /**
     * 脱敏手机号
     */
    public static String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 脱敏邮箱
     */
    public static String maskEmail(String email) {
        if (StrUtil.isBlank(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String username = parts[0];
        if (username.length() <= 2) {
            return "**@" + parts[1];
        }
        return username.substring(0, 2) + "****@" + parts[1];
    }
}

