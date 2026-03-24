package com.example.camerarental.common.annotation;

import java.lang.annotation.*;

/**
 * 防重复提交注解
 * 基于 Redis 实现，在指定时间间隔内同一用户同一接口只能提交一次
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {

    /**
     * 防重间隔时间（秒），默认 3 秒
     */
    int interval() default 3;

    /**
     * 提示消息
     */
    String message() default "请勿重复提交";
}

