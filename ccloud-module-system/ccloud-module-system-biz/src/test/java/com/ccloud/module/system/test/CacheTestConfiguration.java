package com.ccloud.module.system.test;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * 需要验证缓存命中与失效的测试引入：用内存缓存替代 Redis。
 * 数据库事务回滚不会回滚缓存，{@link BaseSystemDbUnitTest} 在每个用例前清空。
 *
 * <p>必须是 @TestConfiguration：普通 @Configuration 位于应用包下，会被 SystemServerApplicationTest 的组件扫描
 * 拾取，顶替掉生产的 Redis CacheManager。
 */
@TestConfiguration(proxyBeanMethods = false)
@EnableCaching
public class CacheTestConfiguration {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
