package com.ccloud.framework.security.config;

import com.ccloud.framework.common.enums.RpcConstants;
import com.ccloud.framework.security.core.filter.LoginUserFilter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/**
 * Spring Security 主配置。
 *
 * <p>业务服务是纯资源服务器：不管理会话、不做登录页、不处理凭据，
 * 身份完全来自网关下发的内部头。因此关闭 CSRF、表单登录与 HTTP Basic，
 * 会话策略设为 STATELESS。
 */
@Slf4j
@EnableWebSecurity
public class CcloudWebSecurityConfigurerAdapter {

    @Resource
    private SecurityProperties securityProperties;
    @Resource
    private org.springframework.security.web.AuthenticationEntryPoint authenticationEntryPoint;
    @Resource
    private AccessDeniedHandler accessDeniedHandler;
    @Resource
    private LoginUserFilter loginUserFilter;
    /**
     * 各模块追加的白名单。用 ObjectProvider 而非直接注入 List：
     * 一个模块可以完全不声明自定义白名单，此时 Spring 找不到任何候选 Bean，
     * 直接注入 List 会导致服务启动失败。
     */
    @Resource
    private org.springframework.beans.factory.ObjectProvider<AuthorizeRequestsCustomizer> authorizeRequestsCustomizers;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer -> AbstractHttpConfigurer.disable())
                .cors(Customizer.withDefaults())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(registry -> {
                    // 文档与健康检查：是否暴露由各自的配置开关决定，不在这里二次把关
                    registry.requestMatchers("/doc.html", "/swagger-ui.html", "/swagger-ui/**",
                            "/v3/api-docs/**", "/webjars/**", "/favicon.ico", "/error").permitAll();
                    registry.requestMatchers("/actuator", "/actuator/**").permitAll();
                    // OPTIONS 预检不携带身份，必须放行
                    registry.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    // RPC 接口的外部访问已由网关阻断，此处放行内部调用
                    registry.requestMatchers(RpcConstants.RPC_API_PREFIX + "/**").permitAll();
                    securityProperties.getPermitAllUrls().forEach(url -> registry.requestMatchers(url).permitAll());
                    // 各模块追加自己的白名单
                    authorizeRequestsCustomizers.orderedStream()
                            .forEach(customizer -> customizer.customize(registry));
                    registry.anyRequest().authenticated();
                })
                .addFilterBefore(loginUserFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
