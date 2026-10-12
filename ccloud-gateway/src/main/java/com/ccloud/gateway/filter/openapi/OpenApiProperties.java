package com.ccloud.gateway.filter.openapi;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 开放接口签名与应用缓存配置。应用由 open 服务管理，网关经 RPC 读取
 */
@ConfigurationProperties(prefix = "ccloud.open")
@Data
public class OpenApiProperties {

    /**
     * 请求时间戳允许的偏差。nonce 只需保存该时长的两倍，超出时间窗的重放由时间戳拒绝
     */
    private Duration timestampTolerance = Duration.ofMinutes(5);

    /**
     * 参与签名的请求体上限。请求体要整体读入内存计算摘要
     */
    private long maxBodySize = 1024 * 1024;

    /**
     * 应用快照缓存时长。open 变更应用后会广播失效，此值只在广播丢失时兜底
     */
    private Duration appCacheTtl = Duration.ofSeconds(60);

    /**
     * 不存在的应用的缓存时长
     */
    private Duration appCacheNotFoundTtl = Duration.ofSeconds(10);

    private long appCacheMaxSize = 10_000;

    /**
     * 调用 open 查询应用的超时
     */
    private Duration appLookupTimeout = Duration.ofSeconds(3);
}
