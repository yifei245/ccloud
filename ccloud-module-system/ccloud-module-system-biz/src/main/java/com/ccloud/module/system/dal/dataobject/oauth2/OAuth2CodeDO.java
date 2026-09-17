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
 * 授权码，一次性使用，有效期 5 分钟
 */
@TableName(value = "system_oauth2_code", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class OAuth2CodeDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String code;
    private Long userId;
    private Integer userType;
    private String clientId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> scopes;
    private LocalDateTime expiresTime;
    /**
     * 换取令牌时必须与签发时一致
     */
    private String redirectUri;
    private String state;
}
