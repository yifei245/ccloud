package com.ccloud.module.system.service.oauth2;

import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2ClientDO;

import java.util.Collection;
import java.util.Map;

/**
 * OAuth2 授权码模式的授权环节：校验授权请求、处理用户同意并签发授权码。
 * 供 OAuth2 开放端点与 SSO 端点共用
 */
public interface OAuth2AuthorizeService {

    /**
     * 校验授权请求：仅支持 response_type=code，客户端须开启授权码模式，申请范围不越界，
     * 重定向地址已登记（精确匹配），防止授权码被发往任意地址
     */
    OAuth2ClientDO validateAuthorizeRequest(String responseType, String clientId, Collection<String> scopes,
                                            String redirectUri);

    /**
     * 校验授权请求后签发授权码
     *
     * @param scopes      范围 → 是否同意
     * @param autoApprove true 时仅尝试自动授权（客户端自动授权范围或用户此前已同意），不记录用户选择
     * @return 应跳转的地址：携带授权码，或用户拒绝时携带 error=access_denied；
     * 自动授权未通过（需用户确认）时返回 null
     */
    String authorize(Long userId, Integer userType, String responseType, String clientId,
                     Map<String, Boolean> scopes, String redirectUri, String state, boolean autoApprove);
}
