package com.ccloud.framework.common.util.cache;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * 本地缓存构建工具
 */
public class CacheUtils {

    /**
     * 构建异步刷新的本地缓存。
     *
     * <p>用 refreshAfterWrite 而非 expireAfterWrite：到期后首个请求拿到的是旧值
     * 并触发后台刷新，不会出现缓存击穿导致的请求堆积。
     *
     * @param duration 刷新间隔
     * @param loader   加载逻辑
     */
    public static <K, V> LoadingCache<K, V> buildAsyncReloadingCache(Duration duration, CacheLoader<K, V> loader) {
        return Caffeine.newBuilder()
                .refreshAfterWrite(duration)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build(loader);
    }

    /**
     * 构建带最大容量与过期时间的本地缓存
     */
    public static <K, V> LoadingCache<K, V> buildCache(Duration duration, long maximumSize, CacheLoader<K, V> loader) {
        return Caffeine.newBuilder()
                .expireAfterWrite(duration)
                .maximumSize(maximumSize)
                .build(loader);
    }
}
