package com.example.camerarental.user.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-minutes}")
    private long expirationMinutes;

    /** RefreshToken 有效期，默认 7 天 */
    @Value("${jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    // ==================== AccessToken ====================

    /**
     * 生成短期 AccessToken（用于接口鉴权）
     */
    public String generateToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationMinutes * 60);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .addClaims(claims)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    // ==================== RefreshToken ====================

    /**
     * 生成长期 RefreshToken 并存入 Redis（用于无感刷新 AccessToken）
     *
     * @param userId 用户ID
     * @return RefreshToken 字符串
     */
    public String generateRefreshToken(Long userId) {
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        String redisKey = REFRESH_TOKEN_PREFIX + userId;

        // 存入 Redis，value = refreshToken，过期时间 = refreshExpirationDays 天
        stringRedisTemplate.opsForValue().set(
                redisKey, refreshToken, refreshExpirationDays, TimeUnit.DAYS
        );

        log.debug("RefreshToken generated for userId={}, ttl={}d", userId, refreshExpirationDays);
        return refreshToken;
    }

    /**
     * 校验 RefreshToken：存在、未过期、与 Redis 中一致
     *
     * @param userId       用户ID
     * @param refreshToken 客户端传入的 RefreshToken
     * @return 校验通过返回 true
     */
    public boolean validateRefreshToken(Long userId, String refreshToken) {
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        String storedToken = stringRedisTemplate.opsForValue().get(redisKey);

        if (storedToken == null) {
            log.warn("RefreshToken not found in Redis for userId={}", userId);
            return false;
        }
        if (!storedToken.equals(refreshToken)) {
            log.warn("RefreshToken mismatch for userId={}", userId);
            return false;
        }
        return true;
    }

    /**
     * 删除 RefreshToken（退出登录 / 强制下线）
     */
    public void removeRefreshToken(Long userId) {
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        stringRedisTemplate.delete(redisKey);
        log.debug("RefreshToken removed for userId={}", userId);
    }

    // ==================== 解析工具 ====================

    /**
     * 解析 AccessToken 中的 Claims（过期也能解析出 subject）
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // AccessToken 已过期但签名有效，仍返回 Claims（用于 refresh 场景）
            return e.getClaims();
        }
    }

    /**
     * 严格解析 AccessToken，过期则抛异常（用于 WebSocket 等不允许过期 token 的场景）
     */
    public Claims parseTokenStrict(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

