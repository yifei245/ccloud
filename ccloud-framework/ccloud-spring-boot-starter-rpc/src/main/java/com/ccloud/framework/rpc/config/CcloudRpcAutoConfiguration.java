package com.ccloud.framework.rpc.config;

import com.ccloud.framework.rpc.core.decoder.CcloudErrorDecoder;
import com.ccloud.framework.rpc.core.interceptor.InternalRequestInterceptor;
import com.ccloud.framework.security.config.SecurityProperties;
import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Feign 自动配置
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
@EnableConfigurationProperties(SecurityProperties.class)
public class CcloudRpcAutoConfiguration {

    @Bean
    public RequestInterceptor internalRequestInterceptor(SecurityProperties securityProperties) {
        return new InternalRequestInterceptor(securityProperties);
    }

    @Bean
    @ConditionalOnMissingBean(ErrorDecoder.class)
    public ErrorDecoder ccloudErrorDecoder() {
        return new CcloudErrorDecoder();
    }

    /**
     * 默认不重试。
     *
     * <p>Feign 的重试对 GET 之外的方法是危险的——下游可能已经执行成功只是响应超时，
     * 重试会造成重复下单、重复扣款。需要重试的接口应显式声明幂等并单独配置。
     */
    @Bean
    @ConditionalOnMissingBean(Retryer.class)
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }

    @Bean
    @ConditionalOnMissingBean(Logger.Level.class)
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
