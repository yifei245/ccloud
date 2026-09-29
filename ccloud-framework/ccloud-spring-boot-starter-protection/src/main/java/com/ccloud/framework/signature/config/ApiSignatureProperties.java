package com.ccloud.framework.signature.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * 开放 API 签名配置
 */
@ConfigurationProperties(prefix = "ccloud.open")
@Data
public class ApiSignatureProperties {

    private boolean enable = true;

    private Duration tolerance = Duration.ofMinutes(5);

    /**
     * appKey -> appSecret 的兜底配置。
     *
     * <p>P1 阶段 open 模块尚未实现，先从配置读取以便本地自测；
     * P2 接入后改由 {@code ApiSignatureFrameworkService} 走 RPC 查询，
     * 本配置降级为仅供开发环境使用。
     */
    private Map<String, String> apps = Collections.emptyMap();
}
