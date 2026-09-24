package com.ccloud.gateway.config;

import com.ccloud.gateway.filter.openapi.OpenApiProperties;
import com.ccloud.gateway.filter.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({SecurityProperties.class, OpenApiProperties.class})
public class GatewaySecurityConfiguration {

    /**
     * 按服务名负载均衡的 WebClient，用于调用 system 校验令牌
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
