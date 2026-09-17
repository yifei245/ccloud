package com.ccloud.framework.redis.core;

import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

/**
 * 替换 CacheManager 实现的扩展点（如多租户按租户隔离缓存键）。
 *
 * <p>之所以不让其他 starter 直接再注册一个 CacheManager：缓存切面要求容器中只有一个，
 * 多个时第一次调用 {@code @Cacheable} 方法就会失败。
 */
@FunctionalInterface
public interface RedisCacheManagerFactory {

    RedisCacheManager create(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration);
}
