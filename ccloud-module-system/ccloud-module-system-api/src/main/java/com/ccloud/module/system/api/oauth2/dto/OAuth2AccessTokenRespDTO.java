package com.ccloud.module.system.api.oauth2.dto;

import java.time.LocalDateTime;
import lombok.Data;

import java.io.Serializable;

/**
 * 访问令牌
 */
@Data
public class OAuth2AccessTokenRespDTO implements Serializable {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 用户编号
     */
    private Long userId;

    /**
     * 用户类型
     */
    private Integer userType;

    /**
     * 过期时间
     */
    private LocalDateTime expiresTime;
}
