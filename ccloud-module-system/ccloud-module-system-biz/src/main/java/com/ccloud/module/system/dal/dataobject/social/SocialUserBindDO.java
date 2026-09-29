package com.ccloud.module.system.dal.dataobject.social;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 社交账号与本系统用户的绑定
 */
@TableName("system_social_user_bind")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class SocialUserBindDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long userId;
    /**
     * 用户类型，见 UserTypeEnum
     */
    private Integer userType;
    private Integer socialType;
    private Long socialUserId;
}
