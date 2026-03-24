package com.example.camerarental.common.aspect;

import cn.hutool.json.JSONUtil;
import com.example.camerarental.common.annotation.OperationLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@ConditionalOnClass(HttpServletRequest.class)
public class OperationLogAspect {

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
        if (operationLog.saveRequestData()) {
            Object[] args = point.getArgs();
            if (args != null && args.length > 0) {
                log.info("请求参数: {}", JSONUtil.toJsonStr(args));
            }
        }

        Object result = null;
        try {
            // 执行方法
            result = point.proceed();

            // 记录响应参数
            if (operationLog.saveResponseData() && result != null) {
                log.info("响应结果: {}", JSONUtil.toJsonStr(result));
            }

            return result;
        } catch (Exception e) {
            log.error("操作异常: ", e);
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            log.info("执行时间: {}ms", endTime - startTime);
            log.info("======= 操作日志结束 =======");
        }
    }
}

