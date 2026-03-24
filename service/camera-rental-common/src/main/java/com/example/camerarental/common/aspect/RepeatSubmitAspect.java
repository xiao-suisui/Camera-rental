package com.example.camerarental.common.aspect;

import com.example.camerarental.common.annotation.RepeatSubmit;
import com.example.camerarental.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnClass(HttpServletRequest.class)
public class RepeatSubmitAspect {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String REPEAT_KEY_PREFIX = "camera-rental:repeat:";

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint point, RepeatSubmit repeatSubmit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes()).getRequest();

        String key = generateKey(request, point);

        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", repeatSubmit.interval(), TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(success)) {
            log.warn("重复提交拦截: key={}", key);
            throw new BusinessException(429, repeatSubmit.message());
        }

        try {
            return point.proceed();
        } catch (Throwable e) {
            // 业务异常时删除锁，允许用户重试
            stringRedisTemplate.delete(key);
            throw e;
        }
    }

    private String generateKey(HttpServletRequest request, ProceedingJoinPoint point) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isEmpty()) {
            userId = request.getRemoteAddr();
        }
        String method = point.getSignature().toShortString();
        String args = Arrays.hashCode(point.getArgs()) + "";
        return REPEAT_KEY_PREFIX + userId + ":" + method + ":" + args;
    }
}

