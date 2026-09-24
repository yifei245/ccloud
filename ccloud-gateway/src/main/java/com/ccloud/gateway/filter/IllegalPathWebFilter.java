package com.ccloud.gateway.filter;

import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.gateway.util.WebFrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * 拒绝含路径穿越或歧义编码的请求。
 *
 * <p>网关按原始路径匹配路由后原样转发，下游 Tomcat 会再做一次解码与规范化：
 * {@code /admin-api/system/..%2F..%2Frpc-api/...} 在网关看来属于 system 业务路由，
 * 到了下游却变成 {@code /rpc-api/...}，绕过网关对内部接口的阻断。两端对路径理解不一致的请求一律拒绝。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IllegalPathWebFilter implements WebFilter {

    /**
     * 编码后的 . / \ % 与空字符：正常客户端不会在路径里编码这些字符
     */
    private static final String[] ENCODED_BLACKLIST = {"%2e", "%2f", "%5c", "%25", "%00"};

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String rawPath = exchange.getRequest().getURI().getRawPath();
        if (isIllegal(rawPath)) {
            log.warn("[filter] 拒绝可疑路径 {} ip={}", rawPath, exchange.getRequest().getRemoteAddress());
            return WebFrameworkUtils.writeJSON(exchange, HttpStatus.BAD_REQUEST,
                    CommonResult.error(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "请求路径不合法"));
        }
        return chain.filter(exchange);
    }

    static boolean isIllegal(String rawPath) {
        if (rawPath == null) {
            return false;
        }
        // 分号会被 Tomcat 当作路径参数剥离，反斜杠会被部分容器当作分隔符
        if (rawPath.indexOf(';') >= 0 || rawPath.indexOf('\\') >= 0) {
            return true;
        }
        String lower = rawPath.toLowerCase(Locale.ROOT);
        for (String encoded : ENCODED_BLACKLIST) {
            if (lower.contains(encoded)) {
                return true;
            }
        }
        for (String segment : rawPath.split("/", -1)) {
            if (".".equals(segment) || "..".equals(segment)) {
                return true;
            }
        }
        return false;
    }
}
