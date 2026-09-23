package com.ccloud.framework.web.config;

import com.ccloud.framework.operatelog.core.aop.OperateLogAspect;
import com.ccloud.framework.operatelog.core.service.LoggerOperateLogFrameworkServiceImpl;
import com.ccloud.framework.operatelog.core.service.OperateLogFrameworkService;
import com.ccloud.framework.apilog.core.filter.ApiAccessLogFilter;
import com.ccloud.framework.apilog.core.service.*;
import com.ccloud.framework.common.enums.WebFilterOrderEnum;
import com.ccloud.framework.web.core.filter.CacheRequestBodyFilter;
import com.ccloud.framework.web.core.handler.GlobalExceptionHandler;
import com.ccloud.framework.web.core.handler.GlobalResponseBodyHandler;
import com.ccloud.framework.web.core.handler.SecurityExceptionHandler;
import com.ccloud.framework.web.core.util.WebFrameworkUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Web 层自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties(WebProperties.class)
@RequiredArgsConstructor
public class CcloudWebAutoConfiguration {

    private final WebProperties webProperties;

    @Value("${spring.application.name:ccloud}")
    private String applicationName;

    /**
     * 把配置塞进静态工具类，让非 Spring 管理的代码也能读到接口前缀
     */
    @PostConstruct
    public void init() {
        WebFrameworkUtils.setProperties(webProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiAccessLogFrameworkService apiAccessLogFrameworkService() {
        return new LoggerApiAccessLogFrameworkServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiErrorLogFrameworkService apiErrorLogFrameworkService() {
        return new LoggerApiErrorLogFrameworkServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public OperateLogFrameworkService operateLogFrameworkService() {
        return new LoggerOperateLogFrameworkServiceImpl();
    }

    @Bean
    public OperateLogAspect operateLogAspect(OperateLogFrameworkService operateLogFrameworkService) {
        return new OperateLogAspect(operateLogFrameworkService);
    }

    @Bean
    public GlobalExceptionHandler globalExceptionHandler(ApiErrorLogFrameworkService apiErrorLogFrameworkService) {
        return new GlobalExceptionHandler(applicationName, apiErrorLogFrameworkService);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.security.access.AccessDeniedException")
    public SecurityExceptionHandler securityExceptionHandler() {
        return new SecurityExceptionHandler();
    }

    @Bean
    public GlobalResponseBodyHandler globalResponseBodyHandler() {
        return new GlobalResponseBodyHandler();
    }

    @Bean
    public FilterRegistrationBean<CacheRequestBodyFilter> cacheRequestBodyFilter() {
        return createFilterBean(new CacheRequestBodyFilter(), WebFilterOrderEnum.REQUEST_BODY_CACHE_FILTER);
    }

    @Bean
    @ConditionalOnProperty(prefix = "ccloud.access-log", value = "enable", matchIfMissing = true)
    public FilterRegistrationBean<ApiAccessLogFilter> apiAccessLogFilter(
            ApiAccessLogFrameworkService apiAccessLogFrameworkService) {
        return createFilterBean(
                new ApiAccessLogFilter(webProperties, applicationName, apiAccessLogFrameworkService),
                WebFilterOrderEnum.API_ACCESS_LOG_FILTER);
    }

    public static <T extends Filter> FilterRegistrationBean<T> createFilterBean(T filter, Integer order) {
        FilterRegistrationBean<T> bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(order);
        return bean;
    }
}
