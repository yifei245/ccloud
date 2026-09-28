package com.ccloud.gateway.filter.limiter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 限流阈值，可在 Nacos 的 ccloud-gateway.yaml 中调整
 */
@ConfigurationProperties(prefix = "ccloud.gateway.rate-limit")
@Data
public class RateLimitProperties {

    private boolean enabled = true;

    /**
     * 未单独配置的路由使用的阈值
     */
    private Limit defaults = new Limit();

    /**
     * 按路由编号（system、member、open-api-pay 等）单独配置
     */
    private Map<String, Limit> routes = new HashMap<>();

    @Data
    public static class Limit {
        /**
         * 每秒补充的令牌数，即持续 QPS 上限
         */
        private int replenishRate = 100;
        /**
         * 桶容量，即允许的瞬时突发
         */
        private int burstCapacity = 200;
    }

    public Limit getRouteLimit(String routeId) {
        return routes.getOrDefault(routeId, defaults);
    }
}
