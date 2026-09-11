package com.ccloud.framework.security.config;

import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * 各业务模块用它追加自己的免鉴权路径。
 *
 * <p>好处是白名单声明在离接口最近的地方——登录接口的放行规则写在 system 模块，
 * 而不是集中在框架层的一个大列表里，改接口时不容易漏改。
 */
public abstract class AuthorizeRequestsCustomizer
        implements org.springframework.core.Ordered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    public abstract void customize(
            AuthorizeHttpRequestsConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry registry);
}
