package com.ccloud.framework.redis.config;

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
@AutoConfiguration(after = CcloudRedisAutoConfiguration.class)
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

    @Bean
    @Primary
    @ConditionalOnMissingBean(RedisCacheManager.class)
    public RedisCacheManager redisCacheManager(RedisConnectionFactory factory,
                                               RedisCacheConfiguration configuration) {
        RedisCacheWriter writer = RedisCacheWriter.nonLockingRedisCacheWriter(factory);
        return new TimeoutRedisCacheManager(writer, configuration);
    }
}
