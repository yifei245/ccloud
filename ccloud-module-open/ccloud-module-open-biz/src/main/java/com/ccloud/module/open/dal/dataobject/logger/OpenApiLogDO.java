package com.ccloud.module.open.dal.dataobject.logger;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 开放接口调用日志。租户编号为应用所属的租户，由消费方按日志内容写入
 */
@TableName("open_api_log")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class OpenApiLogDO extends TenantBaseDO {

    /**
     * 日志编号
     */
    @TableId
    private Long id;
    /**
     * 应用编号
     */
    private Long appId;
    /**
     * 应用标识，应用已删除或查不到时为空串
     */
    private String appKey;
    /**
     * 接口编码，请求的方法与路径不在接口目录中时为空
     */
    private String apiCode;
    /**
     * 请求方法
     */
    private String requestMethod;
    /**
     * 请求地址
     */
    private String requestUrl;
    /**
     * 请求参数，敏感字段已由发送方脱敏
     */
    private String requestParams;
    /**
     * 业务结果码
     */
    private Integer resultCode;
    /**
     * 结果提示
     */
    private String resultMsg;
    /**
     * 调用方 IP
     */
    private String userIp;
    /**
     * 执行时长，单位毫秒
     */
    private Integer duration;
    /**
     * 开始时间
     */
    private LocalDateTime beginTime;
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    /**
     * 链路追踪编号
     */
    private String traceId;
}
