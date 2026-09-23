package com.ccloud.module.member.framework.security.config;

import com.ccloud.framework.security.config.AuthorizeRequestsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * member 模块的免鉴权路径。网关侧有独立白名单（Task 42），两边都要配
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Bean("memberAuthorizeRequestsCustomizer")
    public AuthorizeRequestsCustomizer authorizeRequestsCustomizer() {
        return new AuthorizeRequestsCustomizer() {
            @Override
            public void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>
                                          .AuthorizationManagerRequestMatcherRegistry registry) {
                // C 端登录、注册、短信验证码、社交登录、刷新令牌
                registry.requestMatchers("/app-api/member/auth/**").permitAll();
            }
        };
    }
}
