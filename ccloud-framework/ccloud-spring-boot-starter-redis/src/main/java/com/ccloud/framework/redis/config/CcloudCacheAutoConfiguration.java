package com.ccloud.framework.redis.config;

import com.ccloud.framework.redis.core.RedisCacheManagerFactory;
import org.springframework.beans.factory.ObjectProvider;
import com.ccloud.framework.redis.core.TimeoutRedisCacheManager;
import com.ccloud.framework.redis.core.util.RedisKeyUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Spring Cache 自动配置，统一走 Redis
 */
// 排在 Boot 的 CacheAutoConfiguration 之前，使其因已存在 CacheManager 而不再创建第二个
@AutoConfiguration(after = CcloudRedisAutoConfiguration.class, beforeName = "org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration")
@EnableConfigurationProperties(CcloudCacheProperties.class)
@EnableCaching
public class CcloudCacheAutoConfiguration {

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(CcloudCacheProperties properties) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(RedisKeyUtils.PREFIX)
                .entryTtl(properties.getDefaultTtl())
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(CcloudRedisAutoConfiguration.buildRedisSerializer()));
        return properties.isCacheNullValues() ? config : config.disableCachingNullValues();
    }

    /**
     * 业务缓存使用的 CacheManager。其他 starter 通过 {@link RedisCacheManagerFactory} 替换实现，而不是另行注册。
     *
     * <p>按名称而非类型判断是否已存在：Spring Cloud LoadBalancer 自带一个内部用的 CacheManager，
     * 按类型判断会因它而跳过本 Bean。标记 @Primary，让缓存切面在存在多个 CacheManager 时选中它。
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "cacheManager")
    public RedisCacheManager cacheManager(RedisConnectionFactory factory, RedisCacheConfiguration configuration,
                                          ObjectProvider<RedisCacheManagerFactory> managerFactory) {
        RedisCacheWriter writer = RedisCacheWriter.nonLockingRedisCacheWriter(factory);
        return managerFactory.getIfAvailable(() -> TimeoutRedisCacheManager::new).create(writer, configuration);
    }
}
