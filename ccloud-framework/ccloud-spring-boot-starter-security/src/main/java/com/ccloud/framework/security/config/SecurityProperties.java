package com.ccloud.framework.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * 安全配置
 */
@ConfigurationProperties(prefix = "ccloud.security")
@Data
public class SecurityProperties {

    /**
     * 内部身份头的签名密钥，必须与网关保持一致。
     * 生产环境通过环境变量注入，不要写进代码仓库。
     */
    private String internalSignSecret;

    /**
     * 内部头时间戳允许的滞后量
     */
    private Duration internalSignTolerance = Duration.ofMinutes(5);

    /**
     * 免鉴权路径。注意这里放行的是"本服务的 Spring Security"，
     * 网关侧还有一份独立的白名单，两者都要配。
     */
    private List<String> permitAllUrls = Collections.emptyList();

    /**
     * 权限判定结果的本地缓存时长。缓存换来的是鉴权不必每次发 RPC，
     * 代价是权限变更后最多延迟这么久才生效。
     */
    private Duration permissionCacheDuration = Duration.ofMinutes(10);

    private long permissionCacheMaxSize = 10_000L;
}
