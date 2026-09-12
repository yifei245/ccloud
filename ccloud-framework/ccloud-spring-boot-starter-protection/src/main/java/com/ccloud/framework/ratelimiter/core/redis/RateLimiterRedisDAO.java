package com.ccloud.framework.ratelimiter.core.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.time.Duration;

/**
 * 基于 Redisson 令牌桶的限流。
 *
 * <p>用 Redisson 的 RRateLimiter 而非自己用 INCR 计数：后者是固定窗口，
 * 在窗口边界处实际可放行两倍流量（窗口末尾打满 + 下个窗口开头再打满）。
 */
@RequiredArgsConstructor
public class RateLimiterRedisDAO {

    private static final String RATE_LIMITER_KEY = "ccloud:rate_limiter:%s";

    private final RedissonClient redissonClient;

    /**
     * @return true 表示获得令牌可以执行；false 表示已达上限
     */
    public boolean tryAcquire(String key, int count, Duration time) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(String.format(RATE_LIMITER_KEY, key));
        rateLimiter.trySetRate(RateType.OVERALL, count, time.toMillis(), RateIntervalUnit.MILLISECONDS);
        // 限流器本身也要过期，否则冷门接口的 key 会永久堆积在 Redis 里
        rateLimiter.expire(time.multipliedBy(2));
        return rateLimiter.tryAcquire();
    }
}
