package com.ccloud.module.pay.framework.security.config;

import com.ccloud.framework.security.config.AuthorizeRequestsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * pay 模块的免鉴权路径。网关侧有独立白名单，两边都要配
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    /**
     * 可匿名访问的接口（不限方法）：渠道回调靠渠道验签保证安全；Mock 确认页只在开启模拟支付的环境可用。
     * 网关的 ccloud.security.ignore-urls 须与此一致，由 GatewayWhitelistConsistencyTest 校验
     */
    public static final String[] ANONYMOUS_URLS = {
            "/app-api/pay/notify/**",
            "/app-api/pay/mock/confirm"
    };

    public static final String[] ANONYMOUS_GET_URLS = {};

    @Bean("payAuthorizeRequestsCustomizer")
    public AuthorizeRequestsCustomizer authorizeRequestsCustomizer() {
        return new AuthorizeRequestsCustomizer() {
            @Override
            public void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>
                                          .AuthorizationManagerRequestMatcherRegistry registry) {
                registry.requestMatchers(ANONYMOUS_URLS).permitAll();
            }
        };
    }
}
