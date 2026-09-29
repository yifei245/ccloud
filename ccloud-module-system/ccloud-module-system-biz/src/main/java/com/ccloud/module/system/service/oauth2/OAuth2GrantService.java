package com.ccloud.module.system.service.oauth2;

import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;

import java.util.List;

/**
 * OAuth2 授权 Service：各授权模式换取访问令牌。
 * 短信验证码、社交登录两种模式分别随短信（Task 35）与社交登录（Task 38）提供
 */
public interface OAuth2GrantService {

    /**
     * 密码模式：管理后台账号密码
     */
    OAuth2AccessTokenDO grantPassword(String username, String password, String clientId, List<String> scopes);

    /**
     * 授权码模式：校验客户端、重定向地址、state 与签发时一致后，一次性消费授权码
     */
    OAuth2AccessTokenDO grantAuthorizationCode(String clientId, String code, String redirectUri, String state);

    OAuth2AccessTokenDO grantRefreshToken(String refreshToken, String clientId);

    /**
     * 客户端模式：令牌代表客户端本身，不代表任何用户（userId 为 0），用于服务端对服务端调用
     */
    OAuth2AccessTokenDO grantClientCredentials(String clientId, List<String> scopes);
}
