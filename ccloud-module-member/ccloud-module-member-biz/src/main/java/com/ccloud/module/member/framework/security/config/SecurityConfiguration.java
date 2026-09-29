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

    /**
     * 可匿名访问的接口。网关的 ccloud.security.ignore-urls 须与此一致，由 GatewayWhitelistConsistencyTest 校验
     */
    public static final String[] ANONYMOUS_AUTH_URLS = {
            "/app-api/member/auth/login",
            "/app-api/member/auth/sms-login",
            "/app-api/member/auth/send-sms-code",
            "/app-api/member/auth/reset-password",
            "/app-api/member/auth/social-auth-redirect",
            "/app-api/member/auth/social-login",
            "/app-api/member/auth/weixin-mini-app-login",
            "/app-api/member/auth/refresh-token",
            "/app-api/member/auth/logout"
    };

    @Bean("memberAuthorizeRequestsCustomizer")
    public AuthorizeRequestsCustomizer authorizeRequestsCustomizer() {
        return new AuthorizeRequestsCustomizer() {
            @Override
            public void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>
                                          .AuthorizationManagerRequestMatcherRegistry registry) {
                // 只放行登录前就要调用的接口。修改密码、绑定社交账号、获取用户信息等必须登录，
                // 不能整体放行 /auth/** 后再靠接口内判空兜底——漏判一处就是越权
                registry.requestMatchers(ANONYMOUS_AUTH_URLS).permitAll();
            }
        };
    }
}
