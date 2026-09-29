package com.ccloud.framework.idempotent.config;

import com.ccloud.framework.idempotent.core.aop.IdempotentAspect;
import com.ccloud.framework.idempotent.core.keyresolver.DefaultIdempotentKeyResolver;
import com.ccloud.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import com.ccloud.framework.idempotent.core.keyresolver.UserIdempotentKeyResolver;
import com.ccloud.framework.idempotent.core.redis.IdempotentRedisDAO;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

/**
 * 幂等自动配置
 */
@AutoConfiguration
public class CcloudIdempotentAutoConfiguration {

    @Bean
    public IdempotentRedisDAO idempotentRedisDAO(StringRedisTemplate stringRedisTemplate) {
        return new IdempotentRedisDAO(stringRedisTemplate);
    }

    @Bean
    public IdempotentAspect idempotentAspect(List<IdempotentKeyResolver> keyResolvers,
                                             IdempotentRedisDAO idempotentRedisDAO) {
        return new IdempotentAspect(keyResolvers, idempotentRedisDAO);
    }

    @Bean
    public DefaultIdempotentKeyResolver defaultIdempotentKeyResolver() {
        return new DefaultIdempotentKeyResolver();
    }

    @Bean
    public UserIdempotentKeyResolver userIdempotentKeyResolver() {
        return new UserIdempotentKeyResolver();
    }
}
