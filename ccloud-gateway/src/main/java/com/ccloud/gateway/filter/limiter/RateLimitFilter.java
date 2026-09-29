package com.ccloud.gateway.filter.limiter;

import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.gateway.filter.openapi.OpenApiProperties;
import com.ccloud.gateway.filter.openapi.OpenApiSignatureFilter;
import com.ccloud.gateway.filter.security.LoginUser;
import com.ccloud.gateway.util.SecurityFrameworkUtils;
import com.ccloud.gateway.util.WebFrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 限流。基于 Redis 令牌桶（复用 Spring Cloud Gateway 自带的 request_rate_limiter.lua），多个网关实例共享配额。
 *
 * <ul>
 *   <li>普通接口：按"路由 + 调用方"计数，已登录按用户、未登录按客户端 IP——单个调用方刷接口不会耗尽他人配额；</li>
 *   <li>开放接口：按应用计数，阈值取自应用配置。排在验签之后，伪造 appId 的无效请求不会消耗对方配额。</li>
 * </ul>
 * Redis 不可用时放行：限流是保护措施，不能因为它的故障让整个网关不可用
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    /**
     * 排在令牌校验与开放接口验签之后，才能取到用户与应用
     */
    public static final int ORDER = -90;

    private static final String KEY_PREFIX = "ccloud:rate_limit:";

    private final RateLimitProperties properties;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<List<Long>> script;

    public RateLimitFilter(RateLimitProperties properties, ReactiveStringRedisTemplate redisTemplate,
                           @Qualifier("redisRequestRateLimiterScript") RedisScript<List<Long>> script) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.script = script;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (!properties.isEnabled() || route == null) {
            return chain.filter(exchange);
        }
        String id;
        int replenishRate;
        int burstCapacity;
        OpenApiProperties.App app = exchange.getAttribute(OpenApiSignatureFilter.OPEN_APP_ATTRIBUTE);
        if (app != null) {
            id = "open-app:" + app.getAppId();
            replenishRate = app.getRateLimit();
            burstCapacity = app.getRateLimit();
        } else {
            RateLimitProperties.Limit limit = properties.getRouteLimit(route.getId());
            id = "route:" + route.getId() + ":" + caller(exchange);
            replenishRate = limit.getReplenishRate();
            burstCapacity = limit.getBurstCapacity();
        }
        return isAllowed(id, replenishRate, burstCapacity).flatMap(allowed -> {
            if (allowed) {
                return chain.filter(exchange);
            }
            exchange.getResponse().getHeaders().set("Retry-After", "1");
            return WebFrameworkUtils.writeJSON(exchange, HttpStatus.TOO_MANY_REQUESTS,
                    CommonResult.error(GlobalErrorCodeConstants.TOO_MANY_REQUESTS));
        });
    }

    private static String caller(ServerWebExchange exchange) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser(exchange);
        if (loginUser != null) {
            return "user:" + loginUser.getUserType() + ":" + loginUser.getId();
        }
        // 取连接的对端地址而不是 X-Forwarded-For：后者可由客户端任意伪造，每次换一个值就能绕过限流
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        return "ip:" + (remote != null && remote.getAddress() != null ? remote.getAddress().getHostAddress() : "unknown");
    }

    private Mono<Boolean> isAllowed(String id, int replenishRate, int burstCapacity) {
        // {} 为 Redis Cluster 的 hash tag，保证同一调用方的两个 key 落在同一槽位
        List<String> keys = List.of(KEY_PREFIX + "{" + id + "}.tokens", KEY_PREFIX + "{" + id + "}.timestamp");
        List<String> args = List.of(String.valueOf(replenishRate), String.valueOf(burstCapacity), "", "1");
        return redisTemplate.execute(script, keys, args)
                .next()
                .map(result -> result.get(0) == 1L)
                .defaultIfEmpty(true)
                .onErrorResume(e -> {
                    log.error("[isAllowed] 限流计数失败，放行 id={}", id, e);
                    return Mono.just(true);
                });
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
