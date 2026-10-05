package com.ccloud.framework.apilog.core.service;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * API 异常日志
 */
@Data
public class ApiErrorLog {

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
    private String userIp;
    private String userAgent;

    private LocalDateTime exceptionTime;
    private String exceptionName;
    private String exceptionMessage;
    private String exceptionRootCauseMessage;
    private String exceptionStackTrace;
    private String exceptionClassName;
    private String exceptionFileName;
    private String exceptionMethodName;
    private Integer exceptionLineNumber;
}
