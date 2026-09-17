package com.ccloud.framework.tenant.config;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.ccloud.framework.common.enums.WebFilterOrderEnum;
import com.ccloud.framework.redis.config.CcloudCacheAutoConfiguration;
import com.ccloud.framework.redis.core.RedisCacheManagerFactory;
import com.ccloud.framework.tenant.core.aop.TenantIgnoreAspect;
import com.ccloud.framework.tenant.core.db.TenantLineHandlerImpl;
import com.ccloud.framework.tenant.core.job.TenantJobAspect;
import com.ccloud.framework.tenant.core.redis.TenantRedisCacheManager;
import com.ccloud.framework.tenant.core.service.TenantFrameworkService;
import com.ccloud.framework.tenant.core.web.TenantContextWebFilter;
import com.ccloud.framework.tenant.core.web.TenantSecurityWebFilter;
import com.ccloud.framework.web.config.CcloudWebAutoConfiguration;
import com.ccloud.framework.web.config.WebProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.util.AntPathMatcher;

/**
 * 多租户自动配置
 */
@AutoConfiguration(after = {CcloudWebAutoConfiguration.class, CcloudCacheAutoConfiguration.class})
@EnableConfigurationProperties(TenantProperties.class)
@ConditionalOnProperty(prefix = "ccloud.tenant", value = "enable", matchIfMissing = true)
public class CcloudTenantAutoConfiguration {

    @Bean
    public TenantIgnoreAspect tenantIgnoreAspect() {
        return new TenantIgnoreAspect();
    }

    @Bean
    @ConditionalOnBean(TenantFrameworkService.class)
    public TenantJobAspect tenantJobAspect(TenantFrameworkService tenantFrameworkService) {
        return new TenantJobAspect(tenantFrameworkService);
    }

    // ========== 数据库层 ==========

    /**
     * 租户拦截器。{@code @Order(-100)} 保证它排在数据权限之前、分页之前——
     * 顺序由 CcloudMybatisAutoConfiguration 统一编排。
     */
    @Bean
    @Order(-100)
    public TenantLineInnerInterceptor tenantLineInnerInterceptor(TenantProperties properties) {
        return new TenantLineInnerInterceptor(new TenantLineHandlerImpl(properties));
    }

    // ========== Web 层 ==========

    @Bean
    public FilterRegistrationBean<TenantContextWebFilter> tenantContextWebFilter() {
        return CcloudWebAutoConfiguration.createFilterBean(
                new TenantContextWebFilter(), WebFilterOrderEnum.TENANT_CONTEXT_FILTER);
    }

    @Bean
    @ConditionalOnBean(TenantFrameworkService.class)
    public FilterRegistrationBean<TenantSecurityWebFilter> tenantSecurityWebFilter(
            TenantProperties tenantProperties, WebProperties webProperties,
            TenantFrameworkService tenantFrameworkService) {
        return CcloudWebAutoConfiguration.createFilterBean(
                new TenantSecurityWebFilter(tenantProperties, webProperties,
                        new AntPathMatcher(), tenantFrameworkService),
                WebFilterOrderEnum.TENANT_SECURITY_FILTER);
    }

    // ========== 缓存层 ==========

    /**
     * 覆盖 redis starter 提供的缓存管理器，加上租户前缀。
     * 数据库隔离了而缓存没隔离，等于隔离形同虚设。
     */
    /**
     * 缓存键按租户隔离：通过工厂替换全局唯一 CacheManager 的实现，不另行注册
     */
    @Bean
    @ConditionalOnClass(RedisCacheManager.class)
    public RedisCacheManagerFactory tenantRedisCacheManagerFactory(TenantProperties properties) {
        return (writer, configuration) ->
                new TenantRedisCacheManager(writer, configuration, properties.getIgnoreCaches());
    }
}
