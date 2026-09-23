package com.ccloud.module.system.dal.dataobject.social;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import com.ccloud.framework.mybatis.core.type.EncryptTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 社交客户端：各租户在社交平台申请的应用。同一租户、平台、用户类型只允许一个
 */
@TableName(value = "system_social_client", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "clientSecret")
@Accessors(chain = true)
public class SocialClientDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String name;
    /**
     * 社交平台，见 SocialTypeEnum
     */
    private Integer socialType;
    /**
     * 用户类型，见 UserTypeEnum
     */
    private Integer userType;
    /**
     * 平台分配的应用编号（appKey / appId）
     */
    private String clientId;
    /**
     * 应用密钥，加密存储
     */
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String clientSecret;
    /**
     * 代理编号，企业微信使用
     */
    private String agentId;
    /**
     * 状态，见 CommonStatusEnum
     */
    private Integer status;
}
