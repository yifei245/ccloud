package com.ccloud.module.system.dal.dataobject.oauth2;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * OAuth2 客户端，全局共享
 */
@TableName(value = "system_oauth2_client", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "secret")
@Accessors(chain = true)
public class OAuth2ClientDO extends BaseDO {

    @TableId
    private Long id;
    private String clientId;
    private String secret;
    private String name;
    private String logo;
    private String description;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    private Integer accessTokenValiditySeconds;
    /**
     * 小于等于 0 表示不签发刷新令牌
     */
    private Integer refreshTokenValiditySeconds;
    /**
     * 可重定向地址，必须精确匹配
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> redirectUris;
    /**
     * 枚举 {@link com.ccloud.module.system.enums.oauth2.OAuth2GrantTypeEnum}
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> authorizedGrantTypes;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> scopes;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> autoApproveScopes;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> authorities;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> resourceIds;
    private String additionalInformation;
}
