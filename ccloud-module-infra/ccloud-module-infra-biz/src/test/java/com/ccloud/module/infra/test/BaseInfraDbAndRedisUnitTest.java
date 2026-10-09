package com.ccloud.module.infra.test;

import com.ccloud.framework.test.core.ut.LocalRedis;
import com.ccloud.framework.redis.config.CcloudRedisAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 在数据库单测基类之上加入真实 Redis：使用本机已有的 Redis（见 LocalRedis）
 */
@ImportAutoConfiguration(classes = {RedisAutoConfiguration.class, CcloudRedisAutoConfiguration.class})
public abstract class BaseInfraDbAndRedisUnitTest extends BaseInfraDbUnitTest {

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        LocalRedis.register(registry);
    }
}
