package com.ccloud.framework.signature.config;

import com.ccloud.framework.signature.core.ApiSignatureFrameworkService;
import com.ccloud.framework.signature.core.ApiSignatureVerifier;
import com.ccloud.framework.signature.core.ConfigApiSignatureFrameworkServiceImpl;
import com.ccloud.framework.signature.core.aop.ApiSignatureAspect;
import com.ccloud.framework.signature.core.redis.ApiSignatureRedisDAO;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 开放 API 签名自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties(ApiSignatureProperties.class)
@ConditionalOnProperty(prefix = "ccloud.open", value = "enable", matchIfMissing = true)
public class CcloudApiSignatureAutoConfiguration {

    @Bean
    public ApiSignatureVerifier apiSignatureVerifier(ApiSignatureProperties properties) {
        return new ApiSignatureVerifier(properties.getTolerance());
    }

    @Bean
    public ApiSignatureRedisDAO apiSignatureRedisDAO(StringRedisTemplate stringRedisTemplate) {
        return new ApiSignatureRedisDAO(stringRedisTemplate);
    }

    /**
     * 仅在 open 模块未提供 RPC 实现时生效
     */
    @Bean
    @ConditionalOnMissingBean(ApiSignatureFrameworkService.class)
    public ApiSignatureFrameworkService configApiSignatureFrameworkService(ApiSignatureProperties properties) {
        return new ConfigApiSignatureFrameworkServiceImpl(properties);
    }

    @Bean
    public ApiSignatureAspect apiSignatureAspect(ApiSignatureVerifier verifier,
                                                 ApiSignatureRedisDAO redisDAO,
                                                 ApiSignatureFrameworkService frameworkService) {
        return new ApiSignatureAspect(verifier, redisDAO, frameworkService);
    }
}
