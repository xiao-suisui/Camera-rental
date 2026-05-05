package com.example.camerarental.common.aspect;

import cn.hutool.json.JSONUtil;
import com.example.camerarental.common.annotation.OperationLog;
import com.example.camerarental.common.entity.OperationLogEntity;
import com.example.camerarental.common.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnClass(HttpServletRequest.class)
@ConditionalOnBean(OperationLogMapper.class)
public class OperationLogAspect {

    private final OperationLogMapper operationLogMapper;

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "oldPassword", "newPassword", "token", "secret", "authorization"
    );

    /**
     * 对参数中的敏感字段进行脱敏（password/token/secret → ***）
     */
    private Object[] sanitizeArgs(Object[] args) {
        if (args == null) return null;
        Object[] sanitized = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            sanitized[i] = sanitizeObject(args[i]);
        }
        return sanitized;
    }

    private Object sanitizeObject(Object obj) {
        if (obj == null) return null;
        Class<?> clazz = obj.getClass();
        if (clazz.isPrimitive() || clazz.getName().startsWith("java.lang.") || clazz.isEnum()) {
            return obj;
        }
        try {
            Object copy = clazz.getDeclaredConstructor().newInstance();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (SENSITIVE_FIELDS.contains(field.getName().toLowerCase())) {
                    field.set(copy, "***");
                } else {
                    field.set(copy, value);
                }
            }
            return copy;
        } catch (Exception e) {
            // 反射失败时返回类型名占位
            return "[" + clazz.getSimpleName() + "]";
        }
    }

    @Around("@annotation(com.example.camerarental.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取注解
        MethodSignature signature = (MethodSignature) point.getSignature();
        OperationLog operationLog = signature.getMethod().getAnnotation(OperationLog.class);

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 记录请求信息
        String className = point.getTarget().getClass().getName();
        String methodName = signature.getName();
        String requestUrl = request != null ? request.getRequestURI() : "";
        String requestMethod = request != null ? request.getMethod() : "";
        String userAgent = request != null ? request.getHeader("User-Agent") : "";

        log.info("======= 操作日志开始 =======");
        log.info("操作类型: {}", operationLog.type());
        log.info("操作描述: {}", operationLog.description());
        log.info("请求URL: {} {}", requestMethod, requestUrl);
        log.info("请求方法: {}.{}", className, methodName);
        log.info("User-Agent: {}", userAgent);

        // 记录请求参数
        Object[] sanitizedArgs = null;
        if (operationLog.saveRequestData()) {
            sanitizedArgs = sanitizeArgs(point.getArgs());
            if (sanitizedArgs != null && sanitizedArgs.length > 0) {
                log.info("请求参数: {}", JSONUtil.toJsonStr(sanitizedArgs));
            }
        }

        Object result = null;
        Exception businessException = null;
        try {
            // 执行方法
            result = point.proceed();

            // 记录响应参数
            if (operationLog.saveResponseData() && result != null) {
                log.info("响应结果: {}", JSONUtil.toJsonStr(result));
            }

            return result;
        } catch (Exception e) {
            businessException = e;
            log.error("操作异常: ", e);
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("执行时间: {}ms", executionTime);
            log.info("======= 操作日志结束 =======");

            try {
                OperationLogEntity logEntity = OperationLogEntity.builder()
                        .type(operationLog.type())
                        .description(operationLog.description())
                        .method(className + "." + methodName)
                        .url(requestUrl)
                        .requestMethod(requestMethod)
                        .requestParams(operationLog.saveRequestData() ? JSONUtil.toJsonStr(sanitizedArgs) : null)
                        .responseResult(operationLog.saveResponseData() && result != null ? JSONUtil.toJsonStr(result) : null)
                        .userId(parseUserId(request))
                        .ip(getClientIp(request))
                        .executionTime(executionTime)
                        .status(businessException == null ? "SUCCESS" : "ERROR")
                        .errorMsg(businessException == null ? null : businessException.getMessage())
                        .createdAt(LocalDateTime.now())
                        .build();
                operationLogMapper.insert(logEntity);
            } catch (Exception ex) {
                // 日志入库失败不应影响主业务。
                log.warn("Operation log persist failed", ex);
            }
        }
    }

    private Long parseUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        try {
            String userId = request.getHeader("X-User-Id");
            return userId == null || userId.isBlank() ? null : Long.valueOf(userId);
        } catch (Exception ex) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

