package com.ccloud.module.system.service.oauth2;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2CodeDO;
import com.ccloud.module.system.enums.oauth2.OAuth2GrantTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
public class OAuth2AuthorizeServiceImpl implements OAuth2AuthorizeService {

    @Resource
    private OAuth2ClientService oauth2ClientService;
    @Resource
    private OAuth2ApproveService oauth2ApproveService;
    @Resource
    private OAuth2CodeService oauth2CodeService;

    @Override
    public OAuth2ClientDO validateAuthorizeRequest(String responseType, String clientId, Collection<String> scopes,
                                                   String redirectUri) {
        // 隐式模式在浏览器地址栏暴露访问令牌，OAuth 2.1 已将其移除
        if (!"code".equals(responseType)) {
            throw exception(BAD_REQUEST.getCode(), "仅支持 response_type=code");
        }
        return oauth2ClientService.validOAuthClientFromCache(clientId, null,
                OAuth2GrantTypeEnum.AUTHORIZATION_CODE.getGrantType(), scopes, redirectUri);
    }

    @Override
    public String authorize(Long userId, Integer userType, String responseType, String clientId,
                            Map<String, Boolean> scopes, String redirectUri, String state, boolean autoApprove) {
        validateAuthorizeRequest(responseType, clientId, scopes.keySet(), redirectUri);

        List<String> approvedScopes;
        if (autoApprove) {
            if (!oauth2ApproveService.checkForPreApproval(userId, userType, clientId, scopes.keySet())) {
                return null;
            }
            approvedScopes = new ArrayList<>(scopes.keySet());
        } else {
            if (!oauth2ApproveService.updateAfterApproval(userId, userType, clientId, scopes)) {
                return buildRedirectUri(redirectUri, Map.of("error", "access_denied",
                        "error_description", "User denied access"), state);
            }
            approvedScopes = CollectionUtils.filterList(new ArrayList<>(scopes.keySet()), scopes::get);
        }
        OAuth2CodeDO code = oauth2CodeService.createAuthorizationCode(userId, userType, clientId, approvedScopes,
                redirectUri, state);
        return buildRedirectUri(redirectUri, Map.of("code", code.getCode()), state);
    }

    private static String buildRedirectUri(String redirectUri, Map<String, String> params, String state) {
        StringBuilder url = new StringBuilder(redirectUri).append(redirectUri.contains("?") ? '&' : '?');
        Map<String, String> all = new LinkedHashMap<>(new TreeMap<>(params));
        if (StrUtil.isNotEmpty(state)) {
            all.put("state", state);
        }
        all.forEach((key, value) -> url.append(key).append('=').append(URLUtil.encodeAll(value)).append('&'));
        return url.substring(0, url.length() - 1);
    }
}
