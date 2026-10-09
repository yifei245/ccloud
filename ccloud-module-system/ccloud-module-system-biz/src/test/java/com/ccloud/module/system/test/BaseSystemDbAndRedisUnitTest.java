package com.ccloud.module.system.test;

import com.ccloud.framework.test.core.ut.LocalRedis;
import com.ccloud.framework.redis.config.CcloudRedisAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 同时需要真实 Redis 的数据库单测基类。使用本机已有的 Redis（见 LocalRedis）
 */
@ImportAutoConfiguration(classes = {RedisAutoConfiguration.class, CcloudRedisAutoConfiguration.class})
public abstract class BaseSystemDbAndRedisUnitTest extends BaseSystemDbUnitTest {

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        LocalRedis.register(registry);
    }
}
