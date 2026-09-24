package com.ccloud.gateway.filter;

import com.ccloud.gateway.filter.security.InternalHeaderWriter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 剥离外部请求携带的内部身份头。下游验签通过就完全信任这些头，
 * 一旦伪造的头穿透网关（例如签名密钥泄露时），任何人都能冒充任意用户。
 *
 * <p>{@code X-Tenant-Id} 不在此剥离：匿名接口（登录、发送验证码）只能靠它确定租户。
 * 已登录请求由 {@code TokenAuthenticationFilter} 以令牌中的租户覆盖
 */
@Component
public class RemoveInternalHeaderFilter implements GlobalFilter, Ordered {

    public static final int ORDER = -200;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        boolean present = InternalHeaderWriter.INTERNAL_HEADERS.stream()
                .anyMatch(exchange.getRequest().getHeaders()::containsKey);
        if (!present) {
            return chain.filter(exchange);
        }
        return chain.filter(exchange.mutate().request(exchange.getRequest().mutate()
                .headers(headers -> InternalHeaderWriter.INTERNAL_HEADERS.forEach(headers::remove))
                .build()).build());
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
