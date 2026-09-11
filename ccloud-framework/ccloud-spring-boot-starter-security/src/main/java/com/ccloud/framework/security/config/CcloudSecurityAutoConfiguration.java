package com.ccloud.framework.security.config;

import com.ccloud.framework.security.core.context.TransmittableThreadLocalSecurityContextHolderStrategy;
import com.ccloud.framework.security.core.filter.LoginUserFilter;
import com.ccloud.framework.security.core.handler.AccessDeniedHandlerImpl;
import com.ccloud.framework.security.core.handler.AuthenticationEntryPointImpl;
import com.ccloud.framework.security.core.service.PermissionFrameworkService;
import com.ccloud.framework.security.core.service.SecurityFrameworkService;
import com.ccloud.framework.security.core.service.SecurityFrameworkServiceImpl;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.function.Supplier;

/**
 * 安全自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnClass(AccessDeniedException.class)
@Import(CcloudWebSecurityConfigurerAdapter.class)
public class CcloudSecurityAutoConfiguration {

    /**
     * 替换默认的 ThreadLocal 策略，使身份能随异步任务传递
     */
    @PostConstruct
    public void initSecurityContextStrategy() {
        SecurityContextHolder.setContextHolderStrategy(
                new TransmittableThreadLocalSecurityContextHolderStrategy());
    }

    /**
     * 强度 4 的 BCrypt。默认强度 10 在本地开发时每次登录要几百毫秒，
     * 而本项目的密码强度由策略与验证码保障，不依赖哈希轮数。
     * 若部署在对暴力破解敏感的环境，调高 {@code ccloud.security.password-encoder-length}。
     */
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(4);
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationEntryPoint.class)
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new AuthenticationEntryPointImpl();
    }

    @Bean
    @ConditionalOnMissingBean(AccessDeniedHandler.class)
    public AccessDeniedHandler accessDeniedHandler() {
        return new AccessDeniedHandlerImpl();
    }

    @Bean
    public LoginUserFilter loginUserFilter(SecurityProperties securityProperties) {
        return new LoginUserFilter(securityProperties);
    }

    /**
     * 权限表达式 Bean，名字固定为 ss 以便 {@code @PreAuthorize("@ss...")} 引用
     */
    @Bean("ss")
    @ConditionalOnBean(PermissionFrameworkService.class)
    public SecurityFrameworkService securityFrameworkService(
            PermissionFrameworkService permissionFrameworkService, SecurityProperties properties) {
        return new SecurityFrameworkServiceImpl(permissionFrameworkService,
                properties.getPermissionCacheDuration(), properties.getPermissionCacheMaxSize());
    }

    /**
     * 把当前登录用户提供给持久层做审计字段填充。
     * 持久层只认 Supplier，不知道 security 的存在。
     */
    @Bean
    @Primary
    @ConditionalOnClass(name = "com.ccloud.framework.mybatis.core.handler.DefaultDBFieldHandler")
    public Supplier<Long> loginUserIdSupplier() {
        return SecurityFrameworkUtils::getLoginUserId;
    }
}
