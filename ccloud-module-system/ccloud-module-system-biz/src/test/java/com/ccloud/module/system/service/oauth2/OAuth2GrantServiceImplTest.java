package com.ccloud.module.system.service.oauth2;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2CodeDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.service.auth.AdminAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2GrantServiceImplTest {

    @InjectMocks
    private OAuth2GrantServiceImpl grantService;
    @Mock
    private OAuth2TokenService tokenService;
    @Mock
    private OAuth2CodeService codeService;
    @Mock
    private AdminAuthService adminAuthService;

    @Test
    void grantPassword_用户名密码正确时签发令牌() {
        when(adminAuthService.authenticate("admin", "admin123")).thenReturn(new AdminUserDO().setId(1L));
        OAuth2AccessTokenDO token = new OAuth2AccessTokenDO().setAccessToken("t");
        when(tokenService.createAccessToken(1L, UserTypeEnum.ADMIN.getValue(), "c", List.of("user.read"))).thenReturn(token);

        assertThat(grantService.grantPassword("admin", "admin123", "c", List.of("user.read"))).isSameAs(token);
    }

    @Test
    void grantPassword_密码错误应抛错误码且不签发() {
        when(adminAuthService.authenticate("admin", "bad")).thenThrow(exception(AUTH_LOGIN_BAD_CREDENTIALS));
        assertThatThrownBy(() -> grantService.grantPassword("admin", "bad", "c", List.of()))
                .extracting("code").isEqualTo(AUTH_LOGIN_BAD_CREDENTIALS.getCode());
        verifyNoInteractions(tokenService);
    }

    @Test
    void grantAuthorizationCode_参数一致时消费授权码并签发() {
        OAuth2CodeDO code = code();
        when(codeService.getAuthorizationCode("abc")).thenReturn(code);
        when(codeService.consumeAuthorizationCode("abc")).thenReturn(code);
        OAuth2AccessTokenDO token = new OAuth2AccessTokenDO();
        when(tokenService.createAccessToken(1L, UserTypeEnum.ADMIN.getValue(), "c", List.of("user.read"))).thenReturn(token);

        assertThat(grantService.grantAuthorizationCode("c", "abc", "http://localhost:8080/cb", "s1")).isSameAs(token);
    }

    @Test
    void grantAuthorizationCode_redirectUri与签发时不一致应拒绝() {
        when(codeService.getAuthorizationCode("abc")).thenReturn(code());
        assertThatThrownBy(() -> grantService.grantAuthorizationCode("c", "abc", "http://evil.com/cb", "s1"))
                .extracting("code").isEqualTo(OAUTH2_GRANT_REDIRECT_URI_MISMATCH.getCode());
        verifyNoInteractions(tokenService);
    }

    @Test
    void grantAuthorizationCode_clientId或state不一致应拒绝() {
        when(codeService.getAuthorizationCode("abc")).thenReturn(code());
        assertThatThrownBy(() -> grantService.grantAuthorizationCode("other", "abc", "http://localhost:8080/cb", "s1"))
                .extracting("code").isEqualTo(OAUTH2_GRANT_CLIENT_ID_MISMATCH.getCode());
        assertThatThrownBy(() -> grantService.grantAuthorizationCode("c", "abc", "http://localhost:8080/cb", "s2"))
                .extracting("code").isEqualTo(OAUTH2_GRANT_STATE_MISMATCH.getCode());
        verify(codeService, never()).consumeAuthorizationCode(any());
        verifyNoInteractions(tokenService);
    }

    @Test
    void grantAuthorizationCode_授权码不存在() {
        when(codeService.getAuthorizationCode("none")).thenReturn(null);
        assertThatThrownBy(() -> grantService.grantAuthorizationCode("c", "none", null, null))
                .extracting("code").isEqualTo(OAUTH2_CODE_NOT_EXISTS.getCode());
    }

    @Test
    void grantClientCredentials_应签发不带userId的令牌() {
        OAuth2AccessTokenDO token = new OAuth2AccessTokenDO();
        when(tokenService.createAccessToken(eq(0L), anyInt(), eq("open"), eq(List.of("trade.order.read")))).thenReturn(token);
        assertThat(grantService.grantClientCredentials("open", List.of("trade.order.read"))).isSameAs(token);
    }

    @Test
    void grantRefreshToken_委托令牌服务() {
        OAuth2AccessTokenDO token = new OAuth2AccessTokenDO();
        when(tokenService.refreshAccessToken("r", "c")).thenReturn(token);
        assertThat(grantService.grantRefreshToken("r", "c")).isSameAs(token);
    }

    private static OAuth2CodeDO code() {
        return new OAuth2CodeDO().setCode("abc").setUserId(1L).setUserType(UserTypeEnum.ADMIN.getValue())
                .setClientId("c").setScopes(List.of("user.read")).setRedirectUri("http://localhost:8080/cb")
                .setState("s1").setExpiresTime(LocalDateTime.now().plusMinutes(5));
    }
}
