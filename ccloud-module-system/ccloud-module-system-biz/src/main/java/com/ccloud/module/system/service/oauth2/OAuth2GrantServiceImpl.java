package com.ccloud.module.system.service.oauth2;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2CodeDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.service.auth.AdminAuthService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Service
public class OAuth2GrantServiceImpl implements OAuth2GrantService {

    /**
     * 客户端模式令牌的用户编号：不代表任何用户，也就不具备任何角色与菜单权限，只能凭 scope 访问
     */
    static final Long CLIENT_CREDENTIALS_USER_ID = 0L;

    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private OAuth2CodeService oauth2CodeService;
    @Resource
    private AdminAuthService adminAuthService;

    @Override
    public OAuth2AccessTokenDO grantPassword(String username, String password, String clientId, List<String> scopes) {
        AdminUserDO user = adminAuthService.authenticate(username, password);
        return oauth2TokenService.createAccessToken(user.getId(), UserTypeEnum.ADMIN.getValue(), clientId, scopes);
    }

    @Override
    public OAuth2AccessTokenDO grantAuthorizationCode(String clientId, String code, String redirectUri, String state) {
        OAuth2CodeDO codeDO = oauth2CodeService.getAuthorizationCode(code);
        if (codeDO == null) {
            throw exception(OAUTH2_CODE_NOT_EXISTS);
        }
        if (!Objects.equals(clientId, codeDO.getClientId())) {
            throw exception(OAUTH2_GRANT_CLIENT_ID_MISMATCH);
        }
        // 签发授权码时带了 redirect_uri，换取令牌时必须原样提供，防止授权码被截获后换到别处
        if (StrUtil.isNotEmpty(codeDO.getRedirectUri()) && !Objects.equals(redirectUri, codeDO.getRedirectUri())) {
            throw exception(OAUTH2_GRANT_REDIRECT_URI_MISMATCH);
        }
        if (StrUtil.isNotEmpty(codeDO.getState()) && !Objects.equals(state, codeDO.getState())) {
            throw exception(OAUTH2_GRANT_STATE_MISMATCH);
        }
        OAuth2CodeDO consumed = oauth2CodeService.consumeAuthorizationCode(code);
        // 令牌归属授权码签发时的租户
        return TenantUtils.execute(consumed.getTenantId(),
                () -> oauth2TokenService.createAccessToken(consumed.getUserId(), consumed.getUserType(),
                        consumed.getClientId(), consumed.getScopes()));
    }

    @Override
    public OAuth2AccessTokenDO grantRefreshToken(String refreshToken, String clientId) {
        return oauth2TokenService.refreshAccessToken(refreshToken, clientId);
    }

    @Override
    public OAuth2AccessTokenDO grantClientCredentials(String clientId, List<String> scopes) {
        return oauth2TokenService.createAccessToken(CLIENT_CREDENTIALS_USER_ID, UserTypeEnum.ADMIN.getValue(),
                clientId, scopes);
    }
}
