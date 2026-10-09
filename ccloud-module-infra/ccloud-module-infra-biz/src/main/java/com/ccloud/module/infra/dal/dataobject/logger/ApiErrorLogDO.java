package com.ccloud.module.infra.dal.dataobject.logger;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * API 异常日志。租户编号为请求所属的租户，由消费方按日志内容写入
 */
@TableName("infra_api_error_log")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class ApiErrorLogDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 链路追踪编号
     */
    private String traceId;
    /**
     * 用户编号，未登录为 0
     */
    private Long userId;
    /**
     * 用户类型
     */
    private Integer userType;
    /**
     * 应用名
     */
    private String applicationName;
    /**
     * 请求方法
     */
    private String requestMethod;
    /**
     * 请求地址
     */
    private String requestUrl;
    /**
     * 请求参数
     */
    private String requestParams;
    /**
     * 用户 IP
     */
    private String userIp;
    /**
     * 浏览器 UA
     */
    private String userAgent;
    /**
     * 异常发生时间
     */
    private LocalDateTime exceptionTime;
    /**
     * 异常名
     */
    private String exceptionName;
    /**
     * 异常信息
     */
    private String exceptionMessage;
    /**
     * 根因异常信息
     */
    private String exceptionRootCauseMessage;
    /**
     * 异常堆栈
     */
    private String exceptionStackTrace;
    /**
     * 异常发生的类
     */
    private String exceptionClassName;
    /**
     * 异常发生的文件
     */
    private String exceptionFileName;
    /**
     * 异常发生的方法
     */
    private String exceptionMethodName;
    /**
     * 异常发生的行号
     */
    private Integer exceptionLineNumber;
    /**
     * 处理状态：0 未处理 1 已处理 2 已忽略
     */
    private Integer processStatus;
    /**
     * 处理时间
     */
    private LocalDateTime processTime;
    /**
     * 处理人编号
     */
    private Integer processUserId;
}
