package com.ccloud.module.system.controller.admin.sso;

import cn.hutool.core.net.url.UrlBuilder;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.system.controller.admin.oauth2.OAuth2OpenController;
import com.ccloud.module.system.controller.admin.oauth2.vo.open.OAuth2OpenAccessTokenRespVO;
import com.ccloud.module.system.controller.admin.oauth2.vo.open.OAuth2OpenCheckTokenRespVO;
import com.ccloud.module.system.controller.admin.sso.vo.SsoAuthorizeRespVO;
import com.ccloud.module.system.controller.admin.sso.vo.SsoLogoutRespVO;
import com.ccloud.module.system.dal.dataobject.logger.LoginLogDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.dal.mysql.oauth2.OAuth2ClientMapper;
import com.ccloud.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import com.ccloud.module.system.enums.logger.LoginLogTypeEnum;
import com.ccloud.module.system.service.auth.AdminAuthService;
import com.ccloud.module.system.service.logger.LoginLogService;
import com.ccloud.module.system.service.oauth2.*;
import com.ccloud.module.system.service.user.AdminUserService;
import com.ccloud.module.system.test.BaseSystemDbAndRedisUnitTest;
import com.ccloud.module.system.test.CacheTestConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static com.ccloud.module.system.service.oauth2.OAuth2TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * SSO 授权码流程：真实数据库与 Redis 串起 SSO 端点、OAuth2 开放端点与全部 OAuth2 服务
 */
@Import({SsoController.class, OAuth2OpenController.class, OAuth2AuthorizeServiceImpl.class,
        OAuth2GrantServiceImpl.class, OAuth2TokenServiceImpl.class, OAuth2ClientServiceImpl.class,
        OAuth2CodeServiceImpl.class, OAuth2ApproveServiceImpl.class, OAuth2AccessTokenRedisDAO.class,
        CacheTestConfiguration.class})
class SsoFlowIntegrationTest extends BaseSystemDbAndRedisUnitTest {

    private static final Long USER_ID = 1L;

    @Resource
    private SsoController ssoController;
    @Resource
    private OAuth2OpenController openController;
    @Resource
    private OAuth2TokenService tokenService;
    @Resource
    private OAuth2ClientMapper clientMapper;

    @MockitoBean
    private AdminUserService userService;
    @MockitoBean
    private AdminAuthService adminAuthService;
    @MockitoBean
    private LoginLogService loginLogService;

    /**
     * 管理后台登录态的访问令牌
     */
    private String consoleToken;

    @BeforeEach
    void seed() {
        clientMapper.insert(client());
        when(userService.getUser(USER_ID)).thenReturn(new AdminUserDO().setId(USER_ID).setUsername("admin").setNickname("管理员"));
        consoleToken = tokenService.createAccessToken(USER_ID, UserTypeEnum.ADMIN.getValue(), CLIENT_ID, null).getAccessToken();
        LoginUser loginUser = new LoginUser();
        loginUser.setId(USER_ID);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        loginUser.setTenantId(TENANT_ID);
        SecurityFrameworkUtils.setLoginUser(loginUser, null);
    }

    @AfterEach
    void clearLogin() {
        SecurityFrameworkUtils.clear();
    }

    @Test
    void 完整授权码流程_authorize拿code再用code换token() {
        SsoAuthorizeRespVO authorize = ssoController.authorize("code", CLIENT_ID, REDIRECT_URI, "user.read", "s1").getData();
        assertThat(authorize.getNeedLogin()).isFalse();
        assertThat(authorize.getRedirectUri()).startsWith(REDIRECT_URI + "?code=").endsWith("&state=s1");

        OAuth2OpenAccessTokenRespVO token = exchangeCode(codeOf(authorize.getRedirectUri()), "s1");
        assertThat(token.getAccessToken()).isNotEmpty();
        assertThat(token.getRefreshToken()).isNotEmpty();
        assertThat(token.getTokenType()).isEqualTo("bearer");
        assertThat(token.getScope()).isEqualTo("user.read");

        OAuth2OpenCheckTokenRespVO check = openController.checkToken(clientRequest(SECRET), token.getAccessToken()).getData();
        assertThat(check.getUserId()).isEqualTo(USER_ID);
        assertThat(check.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(check.getClientId()).isEqualTo(CLIENT_ID);
    }

    @Test
    void 未登录时应返回needLogin且不签发授权码() {
        SecurityFrameworkUtils.clear();
        SsoAuthorizeRespVO authorize = ssoController.authorize("code", CLIENT_ID, REDIRECT_URI, "user.read", "s1").getData();
        assertThat(authorize.getNeedLogin()).isTrue();
        assertThat(authorize.getRedirectUri()).isNull();
        assertThat(authorize.getClient().getName()).isEqualTo("测试客户端");
    }

    @Test
    void code被使用两次时第二次应失败() {
        String code = codeOf(ssoController.authorize("code", CLIENT_ID, REDIRECT_URI, "user.read", null).getData().getRedirectUri());
        exchangeCode(code, null);
        assertServiceException(() -> exchangeCode(code, null), OAUTH2_CODE_NOT_EXISTS);
    }

    @Test
    void redirect_uri不在客户端白名单内应拒绝() {
        assertServiceException(() -> ssoController.authorize("code", CLIENT_ID, "http://localhost:8080/cb/../evil", "user.read", null),
                OAUTH2_CLIENT_REDIRECT_URI_NOT_MATCH);
        // 未登录也先校验，否则登录页登录后会把用户带到任意地址
        SecurityFrameworkUtils.clear();
        assertServiceException(() -> ssoController.authorize("code", CLIENT_ID, "https://evil.com/cb", "user.read", null),
                OAUTH2_CLIENT_REDIRECT_URI_NOT_MATCH);
    }

    @Test
    void 仅支持response_type为code() {
        assertThatThrownBy(() -> ssoController.authorize("token", CLIENT_ID, REDIRECT_URI, "user.read", null))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void client_secret错误时换token应失败() {
        String code = codeOf(ssoController.authorize("code", CLIENT_ID, REDIRECT_URI, "user.read", null).getData().getRedirectUri());
        MockHttpServletRequest request = clientRequest("wrong-secret");
        assertServiceException(() -> openController.postAccessToken(request, "authorization_code", code, REDIRECT_URI,
                null, null, null, null, null), OAUTH2_CLIENT_CLIENT_SECRET_ERROR);
    }

    @Test
    void 未授权的scope应被拒绝() {
        assertServiceException(() -> ssoController.authorize("code", CLIENT_ID, REDIRECT_URI, "user.read admin.all", null),
                OAUTH2_CLIENT_SCOPE_OVER);
    }

    @Test
    void auto_approve_scopes覆盖时应跳过授权页直接返回code_否则须用户确认() {
        // user.read 在自动授权范围内
        assertThat(ssoController.authorize("code", CLIENT_ID, REDIRECT_URI, "user.read", null).getData().getRedirectUri())
                .contains("code=");

        // user.write 不在自动授权范围内，返回授权页信息
        SsoAuthorizeRespVO consent = ssoController.authorize("code", CLIENT_ID, REDIRECT_URI, "user.read user.write", "s1").getData();
        assertThat(consent.getNeedLogin()).isFalse();
        assertThat(consent.getRedirectUri()).isNull();
        assertThat(consent.getScopes()).extracting("key").containsExactly("user.read", "user.write");
        assertThat(consent.getScopes()).extracting("value").containsExactly(true, false);

        // 用户确认后签发授权码，此后同样的申请不再弹授权页
        String redirect = ssoController.approve("code", CLIENT_ID, "{\"user.read\":true,\"user.write\":true}",
                REDIRECT_URI, "s1").getData();
        assertThat(exchangeCode(codeOf(redirect), "s1").getScope()).isEqualTo("user.read user.write");
        assertThat(ssoController.authorize("code", CLIENT_ID, REDIRECT_URI, "user.read user.write", null).getData().getRedirectUri())
                .contains("code=");
    }

    @Test
    void 用户拒绝授权时应带access_denied跳回() {
        String redirect = ssoController.approve("code", CLIENT_ID, "{\"user.write\":false}", REDIRECT_URI, "s1").getData();
        assertThat(redirect).isEqualTo(REDIRECT_URI + "?error=access_denied&error_description=User%20denied%20access&state=s1");
    }

    @Test
    void 统一登出后原access_token应失效() {
        String code = codeOf(ssoController.authorize("code", CLIENT_ID, REDIRECT_URI, "user.read", null).getData().getRedirectUri());
        String clientToken = exchangeCode(code, null).getAccessToken();

        SsoLogoutRespVO logout = ssoController.logout(CLIENT_ID, REDIRECT_URI).getData();
        assertThat(logout.getRedirectUri()).isEqualTo(REDIRECT_URI);
        assertThat(logout.getClientIds()).containsExactly(CLIENT_ID);

        // 管理后台自身与各客户端的令牌全部作废
        assertThat(tokenService.getAccessToken(clientToken)).isNull();
        assertThat(tokenService.getAccessToken(consoleToken)).isNull();
        assertThatThrownBy(() -> openController.checkToken(clientRequest(SECRET), clientToken))
                .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(401);
        verify(loginLogService).createLoginLog(argThat((LoginLogDO log) ->
                LoginLogTypeEnum.LOGOUT_SELF.getType().equals(log.getLogType()) && USER_ID.equals(log.getUserId())));
    }

    @Test
    void 统一登出未登录时应幂等返回_但redirect_uri仍须校验() {
        SecurityFrameworkUtils.clear();
        assertThat(ssoController.logout(CLIENT_ID, REDIRECT_URI).getData().getClientIds()).isEmpty();
        verify(loginLogService, never()).createLoginLog(any());
        assertServiceException(() -> ssoController.logout(CLIENT_ID, "https://evil.com/cb"),
                OAUTH2_CLIENT_REDIRECT_URI_NOT_MATCH);
    }

    @Test
    void refresh_token换新令牌后旧access_token应失效() {
        String code = codeOf(ssoController.authorize("code", CLIENT_ID, REDIRECT_URI, "user.read", null).getData().getRedirectUri());
        OAuth2OpenAccessTokenRespVO old = exchangeCode(code, null);

        OAuth2OpenAccessTokenRespVO refreshed = openController.postAccessToken(clientRequest(SECRET), "refresh_token",
                null, null, null, null, null, null, old.getRefreshToken()).getData();

        assertThat(refreshed.getAccessToken()).isNotEqualTo(old.getAccessToken());
        assertThat(tokenService.getAccessToken(old.getAccessToken())).isNull();
        assertThat(openController.checkToken(clientRequest(SECRET), refreshed.getAccessToken()).getData().getUserId())
                .isEqualTo(USER_ID);
    }

    private OAuth2OpenAccessTokenRespVO exchangeCode(String code, String state) {
        return openController.postAccessToken(clientRequest(SECRET), "authorization_code", code, REDIRECT_URI, state,
                null, null, null, null).getData();
    }

    private static MockHttpServletRequest clientRequest(String secret) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("client_id", CLIENT_ID);
        request.addParameter("client_secret", secret);
        return request;
    }

    private static String codeOf(String redirectUri) {
        assertThat(redirectUri).as("应签发授权码").isNotNull();
        return String.valueOf(UrlBuilder.of(redirectUri).getQuery().get("code"));
    }
}
