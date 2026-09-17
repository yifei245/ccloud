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

/**
 * 刷新令牌
 */
@TableName(value = "system_oauth2_refresh_token", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class OAuth2RefreshTokenDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String refreshToken;
    private Long userId;
    private Integer userType;
    private String clientId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> scopes;
    private LocalDateTime expiresTime;
}
