package com.ccloud.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * 全局 CORS，只在网关处理：下游服务不配置 CORS，避免响应里出现重复的 Allow-Origin 头被浏览器拒绝。
 *
 * <p>身份凭证走 Authorization 头而非 Cookie，允许任意来源不会让第三方网站借用户的登录态发请求；
 * 生产环境仍建议通过 {@code ccloud.gateway.cors.allowed-origin-patterns} 收窄到自己的域名。
 */
@Configuration(proxyBeanMethods = false)
public class CorsConfiguration {

    @Bean
    public CorsWebFilter corsWebFilter(
            @Value("${ccloud.gateway.cors.allowed-origin-patterns:*}") List<String> allowedOriginPatterns) {
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOriginPatterns);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        // 前端从响应头读取链路编号，报障时附上
        config.addExposedHeader("trace-id");
        config.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
