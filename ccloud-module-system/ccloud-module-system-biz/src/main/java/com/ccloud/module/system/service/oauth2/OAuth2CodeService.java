package com.ccloud.module.system.service.oauth2;

import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2CodeDO;

import java.util.List;

/**
 * OAuth2 授权码 Service
 */
public interface OAuth2CodeService {

    /**
     * 签发授权码，有效期 5 分钟
     */
    OAuth2CodeDO createAuthorizationCode(Long userId, Integer userType, String clientId, List<String> scopes,
                                         String redirectUri, String state);

    /**
     * @return 不存在返回 null；不校验是否过期，不作废
     */
    OAuth2CodeDO getAuthorizationCode(String code);

    /**
     * 一次性消费授权码：作废后返回；不存在、已被消费或已过期时抛出异常，过期的同样作废
     */
    OAuth2CodeDO consumeAuthorizationCode(String code);
}
