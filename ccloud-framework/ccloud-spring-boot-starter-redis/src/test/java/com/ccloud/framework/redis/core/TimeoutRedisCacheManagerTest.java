package com.ccloud.framework.redis.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TimeoutRedisCacheManagerTest {

    private TimeoutRedisCacheManager manager;

    @BeforeEach
    void setUp() {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1));
        manager = new TimeoutRedisCacheManager(mock(RedisCacheWriter.class), defaultConfig);
    }

    private Duration ttlOf(String cacheName) {
        RedisCache cache = manager.getMissingCache(cacheName);
        assertThat(cache).isNotNull();
        return cache.getCacheConfiguration().getTtlFunction()
                .getTimeToLive(new Object(), null);
    }

    @Test
    @DisplayName("缓存名带秒级后缀时应解析出对应 TTL")
    void shouldParseSecondsSuffix() {
        assertThat(ttlOf("user#30s")).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("缓存名带分钟后缀时应解析出对应 TTL")
    void shouldParseMinutesSuffix() {
        assertThat(ttlOf("user#5m")).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("缓存名带小时后缀时应解析出对应 TTL")
    void shouldParseHoursSuffix() {
        assertThat(ttlOf("role#2h")).isEqualTo(Duration.ofHours(2));
    }

    @Test
    @DisplayName("缓存名带天级后缀时应解析出对应 TTL")
    void shouldParseDaysSuffix() {
        assertThat(ttlOf("dict#1d")).isEqualTo(Duration.ofDays(1));
    }

    @Test
    @DisplayName("缓存名无后缀时应使用默认 TTL")
    void shouldFallbackToDefaultTtlWithoutSuffix() {
        assertThat(ttlOf("plain")).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("后缀格式非法时应使用默认 TTL 而非抛异常")
    void shouldFallbackToDefaultTtlOnMalformedSuffix() {
        assertThat(ttlOf("weird#abc")).isEqualTo(Duration.ofHours(1));
        assertThat(ttlOf("weird#30x")).isEqualTo(Duration.ofHours(1));
        assertThat(ttlOf("weird#")).isEqualTo(Duration.ofHours(1));
    }
}
