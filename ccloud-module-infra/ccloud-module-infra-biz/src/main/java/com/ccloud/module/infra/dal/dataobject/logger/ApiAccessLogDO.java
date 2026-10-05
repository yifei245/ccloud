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
 * API 访问日志。租户编号为请求所属的租户，由消费方按日志内容写入
 */
@TableName("infra_api_access_log")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class ApiAccessLogDO extends TenantBaseDO {

    /**
     * 日志主键
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
     * 响应结果
     */
    private String responseBody;
    /**
     * 用户 IP
     */
    private String userIp;
    /**
     * 浏览器 UA
     */
    private String userAgent;
    /**
     * 操作模块
     */
    private String operateModule;
    /**
     * 操作名
     */
    private String operateName;
    /**
     * 操作分类
     */
    private Integer operateType;
    /**
     * 开始请求时间
     */
    private LocalDateTime beginTime;
    /**
     * 结束请求时间
     */
    private LocalDateTime endTime;
    /**
     * 执行时长，单位毫秒
     */
    private Integer duration;
    /**
     * 业务结果码
     */
    private Integer resultCode;
    /**
     * 结果提示
     */
    private String resultMsg;
}
