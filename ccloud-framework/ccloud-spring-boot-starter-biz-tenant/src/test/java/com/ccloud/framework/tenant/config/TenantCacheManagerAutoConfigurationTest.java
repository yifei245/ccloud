package com.ccloud.framework.tenant.config;

import com.ccloud.framework.redis.config.CcloudCacheAutoConfiguration;
import com.ccloud.framework.tenant.core.redis.TenantRedisCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 缓存切面按类型取唯一的 CacheManager：存在多个时，第一次调用 @Cacheable 方法就会失败。
 * 引入多租户后必须仍然只有一个，且是按租户隔离键的实现。
 */
class TenantCacheManagerAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
            .withPropertyValues("spring.cache.type=redis")
            .withConfiguration(AutoConfigurations.of(
                    CcloudCacheAutoConfiguration.class,
                    CacheAutoConfiguration.class,
                    CcloudTenantAutoConfiguration.class));

    @Test
    void 引入多租户时只有一个CacheManager且按租户隔离() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).getBeans(CacheManager.class).hasSize(1);
            assertThat(ctx.getBean(CacheManager.class)).isInstanceOf(TenantRedisCacheManager.class);
        });
    }

    /**
     * 模拟 Spring Cloud LoadBalancer 自带的内部 CacheManager：业务缓存必须仍然解析到本框架的实现
     */
    @Test
    void 存在其他组件的CacheManager时业务缓存仍使用租户隔离实现() {
        runner.withBean("loadBalancerCacheManager", CacheManager.class,
                        org.springframework.cache.concurrent.ConcurrentMapCacheManager::new)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean("cacheManager")).isInstanceOf(TenantRedisCacheManager.class);
                    // 缓存切面按类型取 CacheManager，多个时取 @Primary
                    assertThat(ctx.getBean(CacheManager.class)).isInstanceOf(TenantRedisCacheManager.class);
                });
    }

    @Test
    void 关闭多租户时使用普通的带过期时间实现() {
        runner.withPropertyValues("ccloud.tenant.enable=false").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).getBeans(CacheManager.class).hasSize(1);
            assertThat(ctx.getBean(CacheManager.class)).isNotInstanceOf(TenantRedisCacheManager.class);
        });
    }
}
