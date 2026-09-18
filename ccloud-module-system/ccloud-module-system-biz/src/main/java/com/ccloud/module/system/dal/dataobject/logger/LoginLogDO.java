package com.ccloud.module.system.dal.dataobject.logger;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 登录日志：登录成功与各类失败、登出都会记录，用于安全审计
 */
@TableName("system_login_log")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class LoginLogDO extends TenantBaseDO {

    @TableId
    private Long id;
    /**
     * 枚举 {@link com.ccloud.module.system.enums.logger.LoginLogTypeEnum}
     */
    private Integer logType;
    private String traceId;
    /**
     * 登录失败且账号不存在时为 0
     */
    private Long userId;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.UserTypeEnum}
     */
    private Integer userType;
    private String username;
    /**
     * 枚举 {@link com.ccloud.module.system.enums.logger.LoginResultEnum}
     */
    private Integer result;
    private String userIp;
    private String userAgent;
}
