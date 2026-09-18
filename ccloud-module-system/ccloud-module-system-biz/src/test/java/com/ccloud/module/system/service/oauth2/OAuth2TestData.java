package com.ccloud.module.system.service.oauth2;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2ClientDO;

import java.util.List;

/**
 * OAuth2 测试数据
 */
public final class OAuth2TestData {

    public static final String CLIENT_ID = "test-web";
    public static final String SECRET = "test-web-secret";
    public static final String REDIRECT_URI = "http://localhost:8080/cb";

    private OAuth2TestData() {
    }

    public static OAuth2ClientDO client() {
        return new OAuth2ClientDO().setClientId(CLIENT_ID).setSecret(SECRET).setName("测试客户端").setLogo("")
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setAccessTokenValiditySeconds(1800).setRefreshTokenValiditySeconds(86400)
                .setRedirectUris(List.of(REDIRECT_URI))
                .setAuthorizedGrantTypes(List.of("password", "authorization_code", "refresh_token", "client_credentials"))
                .setScopes(List.of("user.read", "user.write"))
                .setAutoApproveScopes(List.of("user.read"))
                .setAuthorities(List.of()).setResourceIds(List.of()).setAdditionalInformation("{}");
    }
}
