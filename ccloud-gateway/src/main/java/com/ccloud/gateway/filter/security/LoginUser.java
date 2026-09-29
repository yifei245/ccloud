package com.ccloud.gateway.filter.security;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 登录用户。字段与 ccloud-spring-boot-starter-security 的 LoginUser 逐一对应——
 * 网关序列化、业务服务反序列化，任何一侧增删字段都要同步修改另一侧
 */
@Data
public class LoginUser {

    private Long id;
    /**
     * 用户类型，见 UserTypeEnum
     */
    private Integer userType;
    private Map<String, String> info;
    private Long tenantId;
    private List<String> scopes;
    private String clientId;
    private LocalDateTime expiresTime;

    public boolean isExpired() {
        return expiresTime != null && expiresTime.isBefore(LocalDateTime.now());
    }
}
