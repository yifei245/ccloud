package com.ccloud.module.infra.framework.security.config;

import com.ccloud.framework.security.config.AuthorizeRequestsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * infra 模块的免鉴权路径。网关侧有独立白名单，两边都要配
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    /**
     * 可匿名访问的接口（不限方法）。网关的 ccloud.security.ignore-urls 须与此一致，由 GatewayWhitelistConsistencyTest 校验
     */
    public static final String[] ANONYMOUS_URLS = {};

    /**
     * 只有 GET 可匿名：文件读取。文件为公开读，地址按内容哈希生成、无法枚举
     */
    public static final String[] ANONYMOUS_GET_URLS = {
            "/admin-api/infra/file/*/get/**"
    };

    @Bean("infraAuthorizeRequestsCustomizer")
    public AuthorizeRequestsCustomizer authorizeRequestsCustomizer() {
        return new AuthorizeRequestsCustomizer() {
            @Override
            public void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>
                                          .AuthorizationManagerRequestMatcherRegistry registry) {
                registry.requestMatchers(HttpMethod.GET, ANONYMOUS_GET_URLS).permitAll();
            }
        };
    }
}
