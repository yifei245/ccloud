package com.ccloud.module.system.service.oauth2;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.oauth2.vo.client.OAuth2ClientPageReqVO;
import com.ccloud.module.system.controller.admin.oauth2.vo.client.OAuth2ClientSaveReqVO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import jakarta.validation.Valid;

import java.util.Collection;

/**
 * OAuth2 客户端 Service
 */
public interface OAuth2ClientService {

    Long createOAuth2Client(@Valid OAuth2ClientSaveReqVO createReqVO);

    void updateOAuth2Client(@Valid OAuth2ClientSaveReqVO updateReqVO);

    void deleteOAuth2Client(Long id);

    OAuth2ClientDO getOAuth2Client(Long id);

    OAuth2ClientDO getOAuth2ClientFromCache(String clientId);

    PageResult<OAuth2ClientDO> getOAuth2ClientPage(OAuth2ClientPageReqVO pageReqVO);

    /**
     * 校验客户端存在且开启
     */
    default OAuth2ClientDO validOAuthClientFromCache(String clientId) {
        return validOAuthClientFromCache(clientId, null, null, null, null);
    }

    /**
     * 逐项校验客户端，参数为空的项不校验，任一项不通过即抛出对应错误码
     *
     * @param clientSecret        客户端密钥
     * @param authorizedGrantType 授权类型
     * @param scopes              申请的授权范围，须为客户端范围的子集
     * @param redirectUri         重定向地址，须与登记的地址完全一致
     */
    OAuth2ClientDO validOAuthClientFromCache(String clientId, String clientSecret, String authorizedGrantType,
                                             Collection<String> scopes, String redirectUri);
}
