package com.ccloud.gateway.filter.openapi;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 开放平台应用。P1 阶段 open 服务尚未实现，应用从 Nacos 配置读取；P2 改为经 RPC 从 open 服务获取
 */
@ConfigurationProperties(prefix = "ccloud.open")
@Data
public class OpenApiProperties {

    private List<App> apps = new ArrayList<>();

    /**
     * 请求时间戳允许的偏差。nonce 只需保存该时长的两倍，超出时间窗的重放由时间戳拒绝
     */
    private Duration timestampTolerance = Duration.ofMinutes(5);

    /**
     * 参与签名的请求体上限。请求体要整体读入内存计算摘要
     */
    private long maxBodySize = 1024 * 1024;

    @Data
    public static class App {
        private String appId;
        private String appSecret;
        /**
         * 应用所属租户，写入下游的 X-Tenant-Id
         */
        private Long tenantId;
        private boolean enabled = true;
        /**
         * 每秒请求数上限，同时作为令牌桶容量。P2 改为取自 open_app.rate_limit
         */
        private int rateLimit = 50;
    }
}
