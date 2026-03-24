package com.example.camerarental.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_POST_PATHS = List.of(
            "/user/register",
            "/user/login",
            "/user/refresh-token"
    );

    private static final List<Pattern> PUBLIC_GET_PATTERNS = List.of(
            Pattern.compile("^/device/search$"),
            Pattern.compile("^/device/\\d+$"),
            Pattern.compile("^/actuator/health(?:/.*)?$")
    );

    private static final List<String> MANAGEMENT_PATH_PATTERNS = List.of(
            "/doc.html",
            "/favicon.ico",
            "/static/favicon.ico",
            "/webjars/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/swagger-config/**",
            "/knife4j/**",
            "/v3/api-docs/**",
            "/**/v3/api-docs/**",
            "/**/druid/**",
            "/druid/**",
            "/actuator/info/**",
            "/actuator/metrics/**",
            "/actuator/prometheus/**",
            "/admin/**"
    );

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${gateway.security.internal-ip-whitelist:127.0.0.1,::1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16}")
    private String internalIpWhitelist;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();
        String clientIp = extractClientIp(request);

        if (isBrowserPreflight(request)) {
            return chain.filter(withGatewayHeaders(exchange, clientIp));
        }

        if (isManagementPath(path)) {
            if (!isAllowedInternalIp(clientIp)) {
                log.warn("Blocked external access to management path: path={}, ip={}", path, clientIp);
                return writeJson(exchange, HttpStatus.FORBIDDEN, 1003, "Forbidden");
            }
            return chain.filter(withGatewayHeaders(exchange, clientIp));
        }

        if (isPublicEndpoint(path, method)) {
            return chain.filter(withGatewayHeaders(exchange, clientIp));
        }

        String authorization = request.getHeaders().getFirst(HEADER_AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(HEADER_BEARER_PREFIX)) {
            return writeJson(exchange, HttpStatus.UNAUTHORIZED, 1002, "Unauthorized");
        }

        try {
            String token = authorization.substring(HEADER_BEARER_PREFIX.length());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Role", claims.get("role", String.class))
                    .header("X-Gateway-Request", "true")
                    .header("X-Client-IP", clientIp)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (ExpiredJwtException e) {
            return writeJson(exchange, HttpStatus.UNAUTHORIZED, 2005, "TOKEN_EXPIRED");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return writeJson(exchange, HttpStatus.UNAUTHORIZED, 1002, "Unauthorized");
        }
    }

    private boolean isPublicEndpoint(String path, String method) {
        if (HttpMethod.POST.name().equalsIgnoreCase(method) && PUBLIC_POST_PATHS.contains(path)) {
            return true;
        }
        if (HttpMethod.GET.name().equalsIgnoreCase(method)) {
            return PUBLIC_GET_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(path).matches());
        }
        return false;
    }

    private boolean isManagementPath(String path) {
        return MANAGEMENT_PATH_PATTERNS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private boolean isBrowserPreflight(ServerHttpRequest request) {
        return HttpMethod.OPTIONS.equals(request.getMethod())
                && StringUtils.hasText(request.getHeaders().getOrigin())
                && StringUtils.hasText(request.getHeaders().getFirst("Access-Control-Request-Method"));
    }

    private ServerWebExchange withGatewayHeaders(ServerWebExchange exchange, String clientIp) {
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Gateway-Request", "true")
                .header("X-Client-IP", clientIp)
                .build();
        return exchange.mutate().request(mutatedRequest).build();
    }

    private String extractClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }

        if (request.getRemoteAddress() == null || request.getRemoteAddress().getAddress() == null) {
            return "unknown";
        }
        return request.getRemoteAddress().getAddress().getHostAddress();
    }

    private boolean isAllowedInternalIp(String ip) {
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return true;
        }

        List<String> rules = parseWhitelistRules(internalIpWhitelist);
        return rules.stream().anyMatch(rule -> ipMatchesRule(ip, rule));
    }

    private List<String> parseWhitelistRules(String rawRules) {
        if (!StringUtils.hasText(rawRules)) {
            return Collections.emptyList();
        }
        String[] split = rawRules.split(",");
        List<String> rules = new ArrayList<>(split.length);
        for (String item : split) {
            String trimmed = item == null ? "" : item.trim();
            if (!trimmed.isEmpty()) {
                rules.add(trimmed);
            }
        }
        return rules;
    }

    private boolean ipMatchesRule(String ip, String rule) {
        if (rule.contains("/")) {
            return isInCidr(ip, rule);
        }
        return rule.equals(ip);
    }

    private boolean isInCidr(String ip, String cidrRule) {
        String[] parts = cidrRule.split("/");
        if (parts.length != 2) {
            return false;
        }
        Long ipLong = ipv4ToLong(ip);
        Long networkLong = ipv4ToLong(parts[0]);
        if (ipLong == null || networkLong == null) {
            return false;
        }

        int prefixLength;
        try {
            prefixLength = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            return false;
        }
        if (prefixLength < 0 || prefixLength > 32) {
            return false;
        }

        long mask = prefixLength == 0 ? 0L : 0xFFFFFFFFL << (32 - prefixLength);
        return (ipLong & mask) == (networkLong & mask);
    }

    private Long ipv4ToLong(String ipv4) {
        String[] parts = ipv4.split("\\.");
        if (parts.length != 4) {
            return null;
        }
        long result = 0L;
        for (String part : parts) {
            int value;
            try {
                value = Integer.parseInt(part);
            } catch (NumberFormatException ex) {
                return null;
            }
            if (value < 0 || value > 255) {
                return null;
            }
            result = (result << 8) | value;
        }
        return result;
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, int code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}", code, message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}

