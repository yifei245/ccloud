package com.ccloud.module.system.dal.dataobject.oauth2;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 访问令牌
 */
@TableName(value = "system_oauth2_access_token", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class OAuth2AccessTokenDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String accessToken;
    /**
     * 为空字符串表示未签发刷新令牌（如客户端模式）
     */
    private String refreshToken;
    /**
     * 客户端模式签发的令牌不代表任何用户，为 0
     */
    private Long userId;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.UserTypeEnum}
     */
    private Integer userType;
    /**
     * 用户信息快照，如昵称、部门编号，随令牌下发给各服务，避免每次回查
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> userInfo;
    private String clientId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> scopes;
    private LocalDateTime expiresTime;
}
