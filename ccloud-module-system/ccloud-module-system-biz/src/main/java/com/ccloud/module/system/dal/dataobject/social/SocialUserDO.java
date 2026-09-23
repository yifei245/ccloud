package com.ccloud.module.system.dal.dataobject.social;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 社交用户：从社交平台拉取的用户信息，一个平台账号一条
 */
@TableName("system_social_user")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = {"token", "rawTokenInfo"})
@Accessors(chain = true)
public class SocialUserDO extends TenantBaseDO {

    @TableId
    private Long id;
    /**
     * 社交平台，见 SocialTypeEnum
     */
    private Integer type;
    private String openid;
    private String token;
    private String rawTokenInfo;
    private String nickname;
    private String avatar;
    private String rawUserInfo;
    /**
     * 最后一次认证的授权码与 state。授权码只能使用一次，同一次授权内的后续步骤（如登录后绑定）据此复用本记录
     */
    private String code;
    private String state;
}
