package com.ccloud.framework.operatelog.core.service;

import lombok.Data;

/**
 * 一条操作日志，字段与 system_operate_log 表对应
 */
@Data
public class OperateLogRecord {

    private String traceId;
    /**
     * 未登录（如定时任务调用）时为 0
     */
    private Long userId;
    private Integer userType;
    /**
     * 操作模块
     */
    private String type;
    /**
     * 操作名
     */
    private String subType;
    /**
     * 业务数据编号，未声明时为 0
     */
    private Long bizId;
    /**
     * 操作内容，失败时带失败原因
     */
    private String action;
    private Boolean success;
    /**
     * JSON：操作类型、参数、返回值、耗时（毫秒），均已脱敏
     */
    private String extra;
    private String requestMethod;
    private String requestUrl;
    private String userIp;
    private String userAgent;
}
