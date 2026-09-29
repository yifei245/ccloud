package com.ccloud.module.system.dal.dataobject.logger;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 操作日志
 */
@TableName("system_operate_log")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class OperateLogDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String traceId;
    private Long userId;
    /**
     * 用户类型，见 UserTypeEnum
     */
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
     * 操作的业务数据编号
     */
    private Long bizId;
    private String action;
    private Boolean success;
    /**
     * JSON：操作类型、参数、返回值、耗时，已脱敏
     */
    private String extra;
    private String requestMethod;
    private String requestUrl;
    private String userIp;
    private String userAgent;
}
