package com.ccloud.module.system.api.social.dto;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.module.system.enums.social.SocialTypeEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 解绑社交账号请求
 */
@Data
public class SocialUserUnbindReqDTO implements Serializable {

    /**
     * 用户编号
     */
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    /**
     * 用户类型
     */
    @NotNull(message = "用户类型不能为空")
    @InEnum(UserTypeEnum.class)
    private Integer userType;

    /**
     * 社交平台类型
     */
    @NotNull(message = "社交平台类型不能为空")
    @InEnum(SocialTypeEnum.class)
    private Integer socialType;

    /**
     * 社交平台 openid
     */
    @NotEmpty(message = "openid 不能为空")
    private String openid;
}
