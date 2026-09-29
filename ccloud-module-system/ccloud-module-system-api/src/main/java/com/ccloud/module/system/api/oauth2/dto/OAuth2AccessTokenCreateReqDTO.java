package com.ccloud.module.system.api.oauth2.dto;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.validation.InEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建访问令牌请求。member 等模块完成身份认证后调用它签发令牌
 */
@Data
public class OAuth2AccessTokenCreateReqDTO implements Serializable {

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
     * 客户端编号
     */
    @NotBlank(message = "客户端编号不能为空")
    private String clientId;

    /**
     * 授权范围
     */
    private List<String> scopes;
}
