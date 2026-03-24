package com.example.camerarental.common.interceptor;

import com.example.camerarental.common.annotation.AdminOnly;
import com.example.camerarental.common.exception.BusinessException;
import com.example.camerarental.common.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员权限拦截器
 * 检查 X-User-Role 请求头是否为 ADMIN
 */
@Slf4j
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 检查方法级别注解
        AdminOnly methodAnnotation = handlerMethod.getMethodAnnotation(AdminOnly.class);
        // 检查类级别注解
        AdminOnly classAnnotation = handlerMethod.getBeanType().getAnnotation(AdminOnly.class);

        if (methodAnnotation == null && classAnnotation == null) {
            return true;
        }

        String role = request.getHeader("X-User-Role");
        if (!"ADMIN".equalsIgnoreCase(role)) {
            String message = methodAnnotation != null ? methodAnnotation.message() : classAnnotation.message();
            log.warn("非管理员访问受限接口: path={}, role={}", request.getRequestURI(), role);
            throw new BusinessException(ErrorCode.FORBIDDEN, message);
        }

        return true;
    }
}


