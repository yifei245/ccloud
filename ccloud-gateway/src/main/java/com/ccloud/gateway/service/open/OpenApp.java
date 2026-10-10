package com.ccloud.gateway.service.open;

import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * open 服务返回的应用快照，与 open 的 OpenAppSnapshotRespDTO 对应。
 * 持有密钥明文，toString 必须排除 appSecret，日志中不得出现完整密钥
 */
@Data
@ToString(exclude = "appSecret")
public class OpenApp {

    private Long id;
    private String appKey;
    private String appSecret;
    private Long tenantId;
    /**
     * 0 开启 1 关闭
     */
    private Integer status;
    private LocalDateTime expireTime;
    private String ipWhitelist;
    private Integer rateLimit;
    private List<Api> apis = List.of();

    @Data
    public static class Api {
        private String method;
        private String path;
    }

    public boolean isEnabled() {
        return Integer.valueOf(0).equals(status);
    }

    public boolean isExpired() {
        return expireTime != null && expireTime.isBefore(LocalDateTime.now());
    }

    /**
     * 按 方法 + 原始路径 精确匹配；不做通配与规范化，路径有任何差异都视为未授权
     */
    public boolean allows(String method, String rawPath) {
        return apis != null && apis.stream().anyMatch(api -> api.getMethod().equalsIgnoreCase(method)
                && api.getPath().equals(rawPath));
    }
}
