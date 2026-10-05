package com.ccloud.framework.apilog.core.service;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * API 访问日志。落库实现在 infra 模块，本类只是跨模块传递的数据载体。
 */
@Data
public class ApiAccessLog {

    private String traceId;
    private Long userId;
    private Integer userType;
    private String applicationName;

    /**
     * 租户编号，取自请求头 X-Tenant-Id，没有时为 null
     */
    private Long tenantId;

    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private String responseBody;
    private String userIp;
    private String userAgent;

    /**
     * 操作模块 / 操作名 / 操作类型，来自 {@code @ApiAccessLog} 注解
     */
    private String operateModule;
    private String operateName;
    private Integer operateType;

    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private Integer duration;

    private Integer resultCode;
    private String resultMsg;

    private Map<String, Object> extra;
}
