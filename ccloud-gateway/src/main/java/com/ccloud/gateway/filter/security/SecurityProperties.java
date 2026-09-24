package com.ccloud.gateway.filter.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "ccloud.security")
@Validated
@Data
public class SecurityProperties {

    /**
     * 与业务服务共享的内部签名密钥。缺失时启动失败：网关签不出身份头，所有登录请求到下游都会变成匿名
     */
    @NotBlank(message = "ccloud.security.internal-sign-secret 不能为空")
    private String internalSignSecret;

    /**
     * 可匿名访问的接口，Ant 风格；以 "GET " 等方法名加空格开头时只匹配该方法。
     * 须与各服务 SecurityConfiguration 的匿名接口一致，由各服务的一致性测试校验
     */
    private List<String> ignoreUrls = new ArrayList<>();

    /**
     * 登出接口：请求结束后立即清除该令牌的本地缓存，不必等缓存过期
     */
    private List<String> logoutUrls = new ArrayList<>();

    /**
     * 令牌校验结果本地缓存时长。期间在其他网关实例或经其他途径被吊销的令牌仍会被放行
     */
    private Duration tokenCacheTtl = Duration.ofSeconds(60);

    private long tokenCacheMaxSize = 10_000;

    /**
     * 调用 system 校验令牌的超时
     */
    private Duration tokenCheckTimeout = Duration.ofSeconds(3);
}
