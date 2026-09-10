package com.ccloud.framework.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 缓存配置
 */
@ConfigurationProperties(prefix = "ccloud.cache")
@Data
public class CcloudCacheProperties {

    /**
     * 未在缓存名中声明 TTL 时使用的默认过期时间
     */
    private Duration redisScanBatchSize = Duration.ofHours(1);

    private Duration defaultTtl = Duration.ofHours(1);

    /**
     * 是否缓存 null 值。开启可防缓存穿透，代价是占用少量额外内存。
     */
    private boolean cacheNullValues = true;
}
