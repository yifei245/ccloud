package com.ccloud.monitor.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * 面板访问控制：除静态资源、登录页与探活接口外全部需要登录。
 *
 * <p>面板能查看各服务的环境变量、日志、线程与堆转储，泄露即等于泄露全部配置密钥。
 * 被监控的服务从注册中心发现，不经 {@code /instances} 主动注册，因此注册接口同样要求登录——
 * 匿名可注册时，任何人都能塞入指向任意地址的实例，面板随后会替他去请求那个地址。
 *
 * <p>页面用表单登录，脚本与命令行用 Basic 认证：Accept 为 JSON 的请求未登录时返回 401，浏览器页面跳转登录页
 */
@Configuration(proxyBeanMethods = false)
public class AdminServerSecurityConfiguration {

    @Bean
    public SecurityFilterChain adminServerSecurityFilterChain(HttpSecurity http, AdminServerProperties adminServer)
            throws Exception {
        String contextPath = adminServer.getContextPath();
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(contextPath + "/");

        http.authorizeHttpRequests(registry -> registry
                        .requestMatchers(antMatcher(contextPath + "/assets/**"),
                                antMatcher(contextPath + "/login"),
                                // 同时匹配 /actuator/health 本身
                                antMatcher(contextPath + "/actuator/health/**"),
                                antMatcher(contextPath + "/actuator/info")).permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage(contextPath + "/login").successHandler(successHandler))
                .logout(logout -> logout.logoutUrl(contextPath + "/logout"))
                .httpBasic(Customizer.withDefaults())
                // 面板前端从 Cookie 读取 CSRF 令牌放入请求头；Basic 认证的脚本调用不带令牌，免于校验
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                antMatcher(HttpMethod.POST, contextPath + "/instances"),
                                antMatcher(HttpMethod.DELETE, contextPath + "/instances/*"),
                                antMatcher(contextPath + "/actuator/**")));
        return http.build();
    }
}
