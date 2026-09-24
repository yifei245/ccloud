package com.ccloud.gateway.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * 网关（WebFlux）侧的安全工具
 */
public final class SecurityFrameworkUtils {

    private static final String BEARER_PREFIX = "Bearer ";

    private SecurityFrameworkUtils() {
    }

    /**
     * 从 {@code Authorization: Bearer xxx} 取访问令牌，其他格式视为未携带
     */
    public static String obtainAccessToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.length() <= BEARER_PREFIX.length()
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
