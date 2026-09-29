package com.ccloud.module.system.enums.oauth2;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * OAuth2 授权类型。前五种为 RFC 6749 标准类型，sms_code 与 social 为本项目扩展
 */
@Getter
@AllArgsConstructor
public enum OAuth2GrantTypeEnum {

    PASSWORD("password", "密码模式"),
    AUTHORIZATION_CODE("authorization_code", "授权码模式"),
    REFRESH_TOKEN("refresh_token", "刷新令牌"),
    CLIENT_CREDENTIALS("client_credentials", "客户端模式"),
    SMS_CODE("sms_code", "短信验证码"),
    SOCIAL("social", "社交登录");

    private final String grantType;
    private final String name;

    public static OAuth2GrantTypeEnum getByGrantType(String grantType) {
        return Arrays.stream(values()).filter(e -> e.grantType.equals(grantType)).findFirst().orElse(null);
    }
}
