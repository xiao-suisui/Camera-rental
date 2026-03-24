package com.example.camerarental.common.annotation;

import java.lang.annotation.*;

/**
 * 管理员权限注解
 * 标注在 Controller 方法上，表示该接口仅允许 ADMIN 角色访问
 * 需配合网关设置的 X-User-Role 请求头使用
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminOnly {

    /**
     * 权限不足时的提示消息
     */
    String message() default "无管理员权限，拒绝访问";
}

