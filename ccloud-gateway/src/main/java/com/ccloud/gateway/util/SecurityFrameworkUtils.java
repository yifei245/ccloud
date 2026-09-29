package com.ccloud.gateway.util;

import com.ccloud.gateway.filter.security.LoginUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

/**
 * 网关（WebFlux）侧的安全工具
 */
public final class SecurityFrameworkUtils {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 已通过令牌校验的登录用户，供后续过滤器（如限流按用户计数）读取
     */
    private static final String LOGIN_USER_ATTRIBUTE = "ccloud.login_user";

    private SecurityFrameworkUtils() {
    }

    public static void setLoginUser(ServerWebExchange exchange, LoginUser loginUser) {
        exchange.getAttributes().put(LOGIN_USER_ATTRIBUTE, loginUser);
    }

    public static LoginUser getLoginUser(ServerWebExchange exchange) {
        return exchange.getAttribute(LOGIN_USER_ATTRIBUTE);
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
