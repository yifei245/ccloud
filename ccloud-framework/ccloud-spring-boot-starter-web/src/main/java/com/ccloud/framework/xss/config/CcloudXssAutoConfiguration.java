package com.ccloud.framework.xss.config;

import com.ccloud.framework.common.enums.WebFilterOrderEnum;
import com.ccloud.framework.web.config.CcloudWebAutoConfiguration;
import com.ccloud.framework.xss.core.JsoupXssCleaner;
import com.ccloud.framework.xss.core.XssCleaner;
import com.ccloud.framework.xss.core.XssFilter;
import com.ccloud.framework.xss.core.XssStringJsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/**
 * XSS 自动配置
 */
@AutoConfiguration(after = CcloudWebAutoConfiguration.class)
@EnableConfigurationProperties(XssProperties.class)
@ConditionalOnProperty(prefix = "ccloud.xss", value = "enable", matchIfMissing = true)
public class CcloudXssAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public XssCleaner xssCleaner() {
        return new JsoupXssCleaner();
    }

    @Bean
    public FilterRegistrationBean<XssFilter> xssFilter(XssProperties properties, PathMatcher pathMatcher,
                                                       XssCleaner xssCleaner) {
        return CcloudWebAutoConfiguration.createFilterBean(
                new XssFilter(properties, pathMatcher, xssCleaner), WebFilterOrderEnum.XSS_FILTER);
    }

    @Bean
    @ConditionalOnMissingBean(PathMatcher.class)
    public PathMatcher pathMatcher() {
        return new AntPathMatcher();
    }

    /**
     * 让 Jackson 在反序列化所有 String 字段时自动做 XSS 清洗
     */
    @Bean
    public SimpleModule xssJacksonModule(XssCleaner xssCleaner) {
        SimpleModule module = new SimpleModule("xss");
        module.addDeserializer(String.class, new XssStringJsonDeserializer(xssCleaner));
        return module;
    }
}
