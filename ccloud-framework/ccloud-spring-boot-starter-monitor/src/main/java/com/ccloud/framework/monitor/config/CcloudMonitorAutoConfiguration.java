package com.ccloud.framework.monitor.config;

import com.ccloud.framework.common.enums.WebFilterOrderEnum;
import com.ccloud.framework.monitor.core.filter.TraceFilter;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * 可观测性自动配置
 */
@AutoConfiguration
public class CcloudMonitorAutoConfiguration {

    /**
     * @param tracerProvider 用 ObjectProvider：未引入追踪组件时也能启动，
     *                       TraceFilter 会退化为自行生成 ID
     */
    @Bean
    public FilterRegistrationBean<TraceFilter> traceFilter(ObjectProvider<Tracer> tracerProvider) {
        FilterRegistrationBean<TraceFilter> bean =
                new FilterRegistrationBean<>(new TraceFilter(tracerProvider.getIfAvailable()));
        bean.setOrder(WebFilterOrderEnum.TRACE_FILTER);
        return bean;
    }
}
