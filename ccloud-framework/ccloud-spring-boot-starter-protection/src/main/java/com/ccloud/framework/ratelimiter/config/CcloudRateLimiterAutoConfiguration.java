package com.ccloud.framework.ratelimiter.config;

import com.ccloud.framework.ratelimiter.core.aop.RateLimiterAspect;
import com.ccloud.framework.ratelimiter.core.keyresolver.*;
import com.ccloud.framework.ratelimiter.core.redis.RateLimiterRedisDAO;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 限流自动配置
 */
@AutoConfiguration
public class CcloudRateLimiterAutoConfiguration {

    @Bean
    public RateLimiterRedisDAO rateLimiterRedisDAO(RedissonClient redissonClient) {
        return new RateLimiterRedisDAO(redissonClient);
    }

    @Bean
    public RateLimiterAspect rateLimiterAspect(List<RateLimiterKeyResolver> keyResolvers,
                                               RateLimiterRedisDAO rateLimiterRedisDAO) {
        return new RateLimiterAspect(keyResolvers, rateLimiterRedisDAO);
    }

    @Bean
    public DefaultRateLimiterKeyResolver defaultRateLimiterKeyResolver() {
        return new DefaultRateLimiterKeyResolver();
    }

    @Bean
    public ClientIpRateLimiterKeyResolver clientIpRateLimiterKeyResolver() {
        return new ClientIpRateLimiterKeyResolver();
    }

    @Bean
    public UserRateLimiterKeyResolver userRateLimiterKeyResolver() {
        return new UserRateLimiterKeyResolver();
    }
}
