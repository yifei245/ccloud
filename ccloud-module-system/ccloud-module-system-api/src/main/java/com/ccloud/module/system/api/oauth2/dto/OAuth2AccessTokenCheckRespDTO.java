package com.ccloud.module.system.api.oauth2.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

import java.io.Serializable;

/**
 * 令牌校验结果。字段与 LoginUser 一一对应，网关据此构造下发给业务服务的内部身份头
 */
@Data
public class OAuth2AccessTokenCheckRespDTO implements Serializable {

    /**
     * 用户编号
     */
    private Long userId;

    /**
     * 用户类型
     */
    private Integer userType;

    /**
     * 用户信息快照
     */
    private Map<String, String> userInfo;

    /**
     * 租户编号
     */
    private Long tenantId;

    /**
     * 客户端编号
     */
    private String clientId;

    /**
     * 授权范围
     */
    private List<String> scopes;

    /**
     * 过期时间
     */
    private LocalDateTime expiresTime;
}
