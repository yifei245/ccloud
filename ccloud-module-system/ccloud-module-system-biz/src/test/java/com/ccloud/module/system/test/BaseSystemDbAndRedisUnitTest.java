package com.ccloud.module.system.test;

import com.ccloud.framework.redis.config.CcloudRedisAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

/**
 * 同时需要真实 Redis 的数据库单测基类。Redis 容器每个 JVM 只启动一次
 */
@ImportAutoConfiguration(classes = {RedisAutoConfiguration.class, CcloudRedisAutoConfiguration.class})
public abstract class BaseSystemDbAndRedisUnitTest extends BaseSystemDbUnitTest {

    private static final GenericContainer<?> REDIS = startRedis();

    @SuppressWarnings("resource")
    private static GenericContainer<?> startRedis() {
        GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
        redis.start();
        return redis;
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
