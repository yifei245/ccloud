package com.ccloud.module.system.framework.security.config;

import com.ccloud.framework.security.config.AuthorizeRequestsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * system 模块的免鉴权路径。
 *
 * <p>注意这里只放行本服务的 Spring Security；网关侧有独立白名单（Task 42），两边都要配。
 * 列入此处的接口必须是"获得身份之前就要访问"的，其余一律要求登录。
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Bean("systemAuthorizeRequestsCustomizer")
    public AuthorizeRequestsCustomizer authorizeRequestsCustomizer() {
        return new AuthorizeRequestsCustomizer() {
            @Override
            public void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>
                                          .AuthorizationManagerRequestMatcherRegistry registry) {
                // 登录、短信登录、发送验证码、忘记密码、注册、社交登录
                registry.requestMatchers(
                        "/admin-api/system/auth/login",
                        "/admin-api/system/auth/sms-login",
                        "/admin-api/system/auth/send-sms-code",
                        "/admin-api/system/auth/reset-password",
                        "/admin-api/system/auth/register",
                        "/admin-api/system/auth/refresh-token",
                        "/admin-api/system/auth/logout",
                        "/admin-api/system/auth/social-auth-redirect",
                        "/admin-api/system/auth/social-login",
                        "/admin-api/system/captcha/**").permitAll();
                // 登录页需要先按租户名或域名解析出租户编号
                registry.requestMatchers(
                        "/admin-api/system/tenant/get-id-by-name",
                        "/admin-api/system/tenant/get-by-website").permitAll();
                // OAuth2 令牌、撤销、校验端点由客户端凭据自行校验（授权端点 /authorize 仍要求用户登录）
                registry.requestMatchers(
                        "/admin-api/system/oauth2/token",
                        "/admin-api/system/oauth2/check-token").permitAll();
                // SSO 授权检查（未登录返回 needLogin）与统一登出（幂等）；确认授权 POST /sso/authorize 仍要求登录
                registry.requestMatchers(HttpMethod.GET,
                        "/admin-api/system/sso/authorize",
                        "/admin-api/system/sso/logout").permitAll();
                // 字典数据在登录页也要用（性别、终端等下拉框）
                registry.requestMatchers("/admin-api/system/dict-data/simple-list").permitAll();
            }
        };
    }
}
