package com.ccloud.framework.idempotent.core.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 幂等标记存取
 */
@RequiredArgsConstructor
public class IdempotentRedisDAO {

    private static final String IDEMPOTENT_KEY = "ccloud:idempotent:%s";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 原子地抢占幂等键
     *
     * @return true 表示抢到，可以执行；false 表示已有相同请求在处理或已完成
     */
    public Boolean setIfAbsent(String key, Duration duration) {
        return stringRedisTemplate.opsForValue()
                .setIfAbsent(formatKey(key), "1", duration);
    }

    public void delete(String key) {
        stringRedisTemplate.delete(formatKey(key));
    }

    private static String formatKey(String key) {
        return String.format(IDEMPOTENT_KEY, key);
    }
}
