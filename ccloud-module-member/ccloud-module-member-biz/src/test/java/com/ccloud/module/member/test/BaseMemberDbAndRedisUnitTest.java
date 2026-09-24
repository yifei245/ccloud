package com.ccloud.module.member.test;

import com.ccloud.framework.redis.config.CcloudRedisAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

/**
 * 在数据库单测基类之上加入真实 Redis（Testcontainers），每个 JVM 只启动一次
 */
@ImportAutoConfiguration(classes = {RedisAutoConfiguration.class, CcloudRedisAutoConfiguration.class})
public abstract class BaseMemberDbAndRedisUnitTest extends BaseMemberDbUnitTest {

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
