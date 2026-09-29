package com.ccloud.module.member.service.auth;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.TerminalEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.member.controller.app.auth.vo.*;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import com.ccloud.module.member.dal.redis.auth.MemberLoginFailCountRedisDAO;
import com.ccloud.module.member.service.level.MemberLevelServiceImpl;
import com.ccloud.module.member.service.user.MemberUserServiceImpl;
import com.ccloud.module.member.test.BaseMemberDbAndRedisUnitTest;
import com.ccloud.module.system.api.logger.LoginLogApi;
import com.ccloud.module.system.api.logger.dto.LoginLogCreateReqDTO;
import com.ccloud.module.system.api.oauth2.OAuth2TokenApi;
import com.ccloud.module.system.api.oauth2.dto.OAuth2AccessTokenCreateReqDTO;
import com.ccloud.module.system.api.oauth2.dto.OAuth2AccessTokenRespDTO;
import com.ccloud.module.system.api.sms.SmsCodeApi;
import com.ccloud.module.system.api.sms.dto.SmsCodeSendReqDTO;
import com.ccloud.module.system.api.sms.dto.SmsCodeUseReqDTO;
import com.ccloud.module.system.api.social.SocialClientApi;
import com.ccloud.module.system.api.social.SocialUserApi;
import com.ccloud.module.system.api.social.dto.SocialUserBindReqDTO;
import com.ccloud.module.system.api.social.dto.SocialUserRespDTO;
import com.ccloud.module.system.api.social.dto.SocialWxPhoneNumberInfoRespDTO;
import com.ccloud.module.system.enums.logger.LoginLogTypeEnum;
import com.ccloud.module.system.enums.logger.LoginResultEnum;
import com.ccloud.module.system.enums.oauth2.OAuth2ClientConstants;
import com.ccloud.module.system.enums.sms.SmsSceneEnum;
import com.ccloud.module.system.enums.social.SocialTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static com.ccloud.module.member.enums.ErrorCodeConstants.*;
import static com.ccloud.module.system.enums.ErrorCodeConstants.SMS_CODE_NOT_FOUND;
import static com.ccloud.module.system.enums.ErrorCodeConstants.SOCIAL_CLIENT_WEIXIN_MINI_APP_PHONE_CODE_ERROR;
import static com.ccloud.module.system.enums.ErrorCodeConstants.SOCIAL_USER_BIND_DUPLICATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 会员认证。system 侧的令牌、验证码、社交能力以 RPC 桩替代：这里验证的是 member 的判断逻辑
 * 与传给 system 的参数；令牌本身的签发、刷新、作废语义由 system 的测试覆盖
 */
@Import({MemberAuthServiceImpl.class, MemberUserServiceImpl.class, MemberLevelServiceImpl.class,
        MemberLoginFailCountRedisDAO.class})
@TestPropertySource(properties = {
        "ccloud.member.auth.login-fail-limit=3",
        "ccloud.member.auth.login-fail-lock-minutes=10",
        "ccloud.member.auth.features=point,sign-in"
})
class MemberAuthServiceImplTest extends BaseMemberDbAndRedisUnitTest {

    private static final MemberLoginContext APP = new MemberLoginContext("10.0.0.1", "ua-app",
            TerminalEnum.APP.getTerminal(), OAuth2ClientConstants.CLIENT_ID_APP);
    private static final MemberLoginContext MINI_APP = new MemberLoginContext("10.0.0.2", "ua-mini",
            TerminalEnum.WECHAT_MINI_PROGRAM.getTerminal(), OAuth2ClientConstants.CLIENT_ID_MP_WEIXIN);

    @Resource
    private MemberAuthServiceImpl authService;
    @Resource
    private MemberUserServiceImpl userService;
    @Resource
    private MemberUserMapper userMapper;

    @MockitoBean
    private OAuth2TokenApi oauth2TokenApi;
    @MockitoBean
    private SmsCodeApi smsCodeApi;
    @MockitoBean
    private SocialUserApi socialUserApi;
    @MockitoBean
    private SocialClientApi socialClientApi;
    @MockitoBean
    private LoginLogApi loginLogApi;

    @BeforeEach
    void stubRpc() {
        when(oauth2TokenApi.createAccessToken(any())).thenAnswer(invocation -> {
            OAuth2AccessTokenCreateReqDTO req = invocation.getArgument(0);
            return CommonResult.success(token("at-" + req.getClientId() + "-" + req.getUserId(), req.getUserId()));
        });
        when(smsCodeApi.useSmsCode(any())).thenReturn(CommonResult.success(true));
        when(smsCodeApi.sendSmsCode(any())).thenReturn(CommonResult.success(true));
        when(loginLogApi.createLoginLog(any())).thenReturn(CommonResult.success(true));
        when(oauth2TokenApi.removeAccessTokensByUser(any(), any())).thenReturn(CommonResult.success(true));
    }

    // ========== 手机号 + 密码 ==========

    @Test
    void login_手机号密码正确时签发MEMBER类型令牌() {
        MemberUserDO user = createUser("13922220001", "pass123");

        AppAuthLoginRespVO resp = authService.login(loginReqVO("13922220001", "pass123"), APP);

        assertThat(resp.getUserId()).isEqualTo(user.getId());
        assertThat(resp.getUserType()).isEqualTo(UserTypeEnum.MEMBER.getValue());
        assertThat(resp.getAccessToken()).isEqualTo("at-ccloud-app-" + user.getId());
        OAuth2AccessTokenCreateReqDTO tokenReq = captureTokenRequest();
        assertThat(tokenReq.getUserType()).isEqualTo(UserTypeEnum.MEMBER.getValue());
        assertThat(tokenReq.getClientId()).isEqualTo(OAuth2ClientConstants.CLIENT_ID_APP);
        MemberUserDO saved = userMapper.selectById(user.getId());
        assertThat(saved.getLoginIp()).isEqualTo("10.0.0.1");
        assertThat(saved.getLoginDate()).isNotNull();
        assertLoginLog(LoginLogTypeEnum.LOGIN_USERNAME, LoginResultEnum.SUCCESS, user.getId());
    }

    @Test
    void login_密码错误应抛BAD_CREDENTIALS() {
        createUser("13922220002", "pass123");

        assertServiceException(() -> authService.login(loginReqVO("13922220002", "wrong"), APP),
                AUTH_LOGIN_BAD_CREDENTIALS);
        verify(oauth2TokenApi, never()).createAccessToken(any());
        assertLoginLog(LoginLogTypeEnum.LOGIN_USERNAME, LoginResultEnum.BAD_CREDENTIALS, null);
    }

    @Test
    void login_手机号未注册应抛BAD_CREDENTIALS_不暴露账号是否存在() {
        assertServiceException(() -> authService.login(loginReqVO("13922220003", "pass123"), APP),
                AUTH_LOGIN_BAD_CREDENTIALS);
    }

    @Test
    void login_未设置密码的用户不能用密码登录() {
        userService.createUserIfAbsent("13922220004", "10.0.0.1", null);

        assertServiceException(() -> authService.login(loginReqVO("13922220004", ""), APP),
                AUTH_LOGIN_BAD_CREDENTIALS);
    }

    @Test
    void login_用户被禁用应抛USER_DISABLED() {
        MemberUserDO user = createUser("13922220005", "pass123");
        userMapper.updateById(new MemberUserDO().setId(user.getId()).setStatus(CommonStatusEnum.DISABLE.getStatus()));

        assertServiceException(() -> authService.login(loginReqVO("13922220005", "pass123"), APP),
                AUTH_LOGIN_USER_DISABLED);
        verify(oauth2TokenApi, never()).createAccessToken(any());
        assertLoginLog(LoginLogTypeEnum.LOGIN_USERNAME, LoginResultEnum.USER_DISABLED, user.getId());
    }

    @Test
    void login_连续失败达到上限后应锁定_正确密码也拒绝() {
        createUser("13922220006", "pass123");
        for (int i = 0; i < 3; i++) {
            assertServiceException(() -> authService.login(loginReqVO("13922220006", "wrong"), APP),
                    AUTH_LOGIN_BAD_CREDENTIALS);
        }

        assertServiceException(() -> authService.login(loginReqVO("13922220006", "pass123"), APP),
                AUTH_LOGIN_FAIL_LOCKED);
        verify(oauth2TokenApi, never()).createAccessToken(any());
    }

    @Test
    void login_成功后应清零失败计数() {
        createUser("13922220007", "pass123");
        for (int i = 0; i < 2; i++) {
            assertServiceException(() -> authService.login(loginReqVO("13922220007", "wrong"), APP),
                    AUTH_LOGIN_BAD_CREDENTIALS);
        }
        authService.login(loginReqVO("13922220007", "pass123"), APP);

        // 清零后再错 2 次仍未达上限 3
        for (int i = 0; i < 2; i++) {
            assertServiceException(() -> authService.login(loginReqVO("13922220007", "wrong"), APP),
                    AUTH_LOGIN_BAD_CREDENTIALS);
        }
        assertThat(authService.login(loginReqVO("13922220007", "pass123"), APP).getAccessToken()).isNotNull();
    }

    // ========== 短信验证码 ==========

    @Test
    void smsLogin_手机号未注册时应自动创建用户并签发令牌() {
        AppAuthLoginRespVO resp = authService.smsLogin(smsLoginReqVO("13922220011", "1234"), MINI_APP);

        MemberUserDO created = userMapper.selectByMobile("13922220011");
        assertThat(created).isNotNull();
        assertThat(created.getRegisterTerminal()).isEqualTo(TerminalEnum.WECHAT_MINI_PROGRAM.getTerminal());
        assertThat(created.getRegisterIp()).isEqualTo("10.0.0.2");
        assertThat(resp.getUserId()).isEqualTo(created.getId());
        assertThat(resp.getUserType()).isEqualTo(UserTypeEnum.MEMBER.getValue());
        SmsCodeUseReqDTO used = captureSmsCodeUse();
        assertThat(used.getScene()).isEqualTo(SmsSceneEnum.MEMBER_LOGIN.getScene());
        assertThat(used.getCode()).isEqualTo("1234");
        assertThat(used.getUsedIp()).isEqualTo("10.0.0.2");
        assertLoginLog(LoginLogTypeEnum.LOGIN_MOBILE, LoginResultEnum.SUCCESS, created.getId());
    }

    @Test
    void smsLogin_验证码错误应抛异常且不创建用户() {
        when(smsCodeApi.useSmsCode(any())).thenReturn(CommonResult.error(SMS_CODE_NOT_FOUND));

        assertThatThrownBy(() -> authService.smsLogin(smsLoginReqVO("13922220012", "0000"), APP))
                .extracting("code").isEqualTo(SMS_CODE_NOT_FOUND.getCode());
        assertThat(userMapper.selectByMobile("13922220012")).isNull();
        verify(oauth2TokenApi, never()).createAccessToken(any());
        assertLoginLog(LoginLogTypeEnum.LOGIN_MOBILE, LoginResultEnum.CAPTCHA_CODE_ERROR, null);
    }

    @Test
    void smsLogin_已注册用户应复用_不重复注册() {
        MemberUserDO user = createUser("13922220013", "pass123");

        AppAuthLoginRespVO resp = authService.smsLogin(smsLoginReqVO("13922220013", "1234"), APP);

        assertThat(resp.getUserId()).isEqualTo(user.getId());
        assertThat(userMapper.selectList(MemberUserDO::getMobile, "13922220013")).hasSize(1);
    }

    @Test
    void smsLogin_禁用用户应拒绝() {
        MemberUserDO user = createUser("13922220014", "pass123");
        userMapper.updateById(new MemberUserDO().setId(user.getId()).setStatus(CommonStatusEnum.DISABLE.getStatus()));

        assertServiceException(() -> authService.smsLogin(smsLoginReqVO("13922220014", "1234"), APP),
                AUTH_LOGIN_USER_DISABLED);
        verify(oauth2TokenApi, never()).createAccessToken(any());
    }

    @Test
    void sendSmsCode_登录场景发往请求的手机号() {
        authService.sendSmsCode(null, smsSendReqVO("13922220021", SmsSceneEnum.MEMBER_LOGIN), "10.0.0.1");

        ArgumentCaptor<SmsCodeSendReqDTO> captor = ArgumentCaptor.forClass(SmsCodeSendReqDTO.class);
        verify(smsCodeApi).sendSmsCode(captor.capture());
        assertThat(captor.getValue().getMobile()).isEqualTo("13922220021");
        assertThat(captor.getValue().getScene()).isEqualTo(SmsSceneEnum.MEMBER_LOGIN.getScene());
        assertThat(captor.getValue().getCreateIp()).isEqualTo("10.0.0.1");
    }

    @Test
    void sendSmsCode_修改密码场景固定发往当前用户手机号_忽略请求中的手机号() {
        MemberUserDO user = createUser("13922220022", "pass123");

        authService.sendSmsCode(user.getId(), smsSendReqVO("13922229999", SmsSceneEnum.MEMBER_UPDATE_PASSWORD),
                "10.0.0.1");

        ArgumentCaptor<SmsCodeSendReqDTO> captor = ArgumentCaptor.forClass(SmsCodeSendReqDTO.class);
        verify(smsCodeApi).sendSmsCode(captor.capture());
        assertThat(captor.getValue().getMobile()).isEqualTo("13922220022");
    }

    @Test
    void sendSmsCode_需登录的场景未登录时应拒绝() {
        assertServiceException(() -> authService.sendSmsCode(null,
                smsSendReqVO("13922220023", SmsSceneEnum.MEMBER_UPDATE_PASSWORD), "10.0.0.1"),
                GlobalErrorCodeConstants.UNAUTHORIZED);
        assertServiceException(() -> authService.sendSmsCode(null,
                smsSendReqVO("13922220023", SmsSceneEnum.MEMBER_UPDATE_MOBILE), "10.0.0.1"),
                GlobalErrorCodeConstants.UNAUTHORIZED);
        verify(smsCodeApi, never()).sendSmsCode(any());
    }

    @Test
    void sendSmsCode_管理后台场景应拒绝() {
        assertServiceException(() -> authService.sendSmsCode(null,
                smsSendReqVO("13922220024", SmsSceneEnum.ADMIN_MEMBER_LOGIN), "10.0.0.1"),
                GlobalErrorCodeConstants.BAD_REQUEST);
        verify(smsCodeApi, never()).sendSmsCode(any());
    }

    @Test
    void sendSmsCode_重置密码场景手机号未注册应拒绝_避免为不存在的账号付费发短信() {
        assertServiceException(() -> authService.sendSmsCode(null,
                smsSendReqVO("13922220025", SmsSceneEnum.MEMBER_RESET_PASSWORD), "10.0.0.1"),
                USER_MOBILE_NOT_EXISTS);
        verify(smsCodeApi, never()).sendSmsCode(any());
    }

    @Test
    void resetPassword_应更新密码并下线该用户全部会话() {
        MemberUserDO user = createUser("13922220031", "old123");
        AppAuthResetPasswordReqVO reqVO = new AppAuthResetPasswordReqVO();
        reqVO.setMobile("13922220031");
        reqVO.setCode("1234");
        reqVO.setPassword("new123");

        authService.resetPassword(reqVO, "10.0.0.1");

        assertThat(userService.isPasswordMatch("new123", userMapper.selectById(user.getId()).getPassword())).isTrue();
        assertThat(captureSmsCodeUse().getScene()).isEqualTo(SmsSceneEnum.MEMBER_RESET_PASSWORD.getScene());
        verify(oauth2TokenApi).removeAccessTokensByUser(user.getId(), UserTypeEnum.MEMBER.getValue());
    }

    @Test
    void updatePassword_验证码按当前用户手机号校验_更新后下线全部会话() {
        MemberUserDO user = createUser("13922220032", "old123");
        AppAuthUpdatePasswordReqVO reqVO = new AppAuthUpdatePasswordReqVO();
        reqVO.setCode("1234");
        reqVO.setPassword("new123");

        authService.updatePassword(user.getId(), reqVO, "10.0.0.1");

        assertThat(userService.isPasswordMatch("new123", userMapper.selectById(user.getId()).getPassword())).isTrue();
        SmsCodeUseReqDTO used = captureSmsCodeUse();
        assertThat(used.getMobile()).isEqualTo("13922220032");
        assertThat(used.getScene()).isEqualTo(SmsSceneEnum.MEMBER_UPDATE_PASSWORD.getScene());
        verify(oauth2TokenApi).removeAccessTokensByUser(user.getId(), UserTypeEnum.MEMBER.getValue());
    }

    // ========== 小程序一键登录 ==========

    @Test
    void weixinMiniAppLogin_首次登录应自动注册并绑定openid() {
        when(socialUserApi.getSocialUserByCode(eq(UserTypeEnum.MEMBER.getValue()),
                eq(SocialTypeEnum.WECHAT_MINI_PROGRAM.getType()), eq("login-code"), anyString()))
                .thenReturn(CommonResult.success(socialUser("openid-1", null)));
        when(socialClientApi.getWxMaPhoneNumberInfo(UserTypeEnum.MEMBER.getValue(), "phone-code"))
                .thenReturn(CommonResult.success(phoneInfo("13922220041")));
        when(socialUserApi.bindSocialUser(any())).thenReturn(CommonResult.success("openid-1"));

        AppAuthLoginRespVO resp = authService.weixinMiniAppLogin(miniAppReqVO(), MINI_APP);

        MemberUserDO created = userMapper.selectByMobile("13922220041");
        assertThat(created).isNotNull();
        assertThat(created.getRegisterTerminal()).isEqualTo(TerminalEnum.WECHAT_MINI_PROGRAM.getTerminal());
        assertThat(resp.getUserId()).isEqualTo(created.getId());
        assertThat(resp.getOpenid()).isEqualTo("openid-1");
        assertThat(captureTokenRequest().getClientId()).isEqualTo(OAuth2ClientConstants.CLIENT_ID_MP_WEIXIN);
        ArgumentCaptor<SocialUserBindReqDTO> bind = ArgumentCaptor.forClass(SocialUserBindReqDTO.class);
        verify(socialUserApi).bindSocialUser(bind.capture());
        assertThat(bind.getValue().getUserId()).isEqualTo(created.getId());
        assertThat(bind.getValue().getUserType()).isEqualTo(UserTypeEnum.MEMBER.getValue());
        assertThat(bind.getValue().getSocialType()).isEqualTo(SocialTypeEnum.WECHAT_MINI_PROGRAM.getType());
        assertThat(bind.getValue().getCode()).isEqualTo("login-code");
        assertLoginLog(LoginLogTypeEnum.LOGIN_SOCIAL, LoginResultEnum.SUCCESS, created.getId());
    }

    @Test
    void weixinMiniAppLogin_二次登录应复用已绑定的用户() {
        MemberUserDO user = createUser("13922220042", "pass123");
        when(socialUserApi.getSocialUserByCode(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(CommonResult.success(socialUser("openid-2", user.getId())));

        AppAuthLoginRespVO resp = authService.weixinMiniAppLogin(miniAppReqVO(), MINI_APP);

        assertThat(resp.getUserId()).isEqualTo(user.getId());
        assertThat(resp.getOpenid()).isEqualTo("openid-2");
        // 已绑定时不再换手机号：微信按次计费，且不应因手机号变更而换号登录
        verify(socialClientApi, never()).getWxMaPhoneNumberInfo(any(), any());
        verify(socialUserApi, never()).bindSocialUser(any());
    }

    @Test
    void weixinMiniAppLogin_phoneCode换手机号失败时应抛异常() {
        when(socialUserApi.getSocialUserByCode(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(CommonResult.success(socialUser("openid-3", null)));
        when(socialClientApi.getWxMaPhoneNumberInfo(any(), any()))
                .thenReturn(CommonResult.error(SOCIAL_CLIENT_WEIXIN_MINI_APP_PHONE_CODE_ERROR));
        long before = userMapper.selectCount();

        assertThatThrownBy(() -> authService.weixinMiniAppLogin(miniAppReqVO(), MINI_APP))
                .extracting("code").isEqualTo(SOCIAL_CLIENT_WEIXIN_MINI_APP_PHONE_CODE_ERROR.getCode());
        assertThat(userMapper.selectCount()).isEqualTo(before);
        verify(socialUserApi, never()).bindSocialUser(any());
        verify(oauth2TokenApi, never()).createAccessToken(any());
    }

    @Test
    void weixinMiniAppLogin_已绑定的用户被禁用应拒绝() {
        MemberUserDO user = createUser("13922220043", "pass123");
        userMapper.updateById(new MemberUserDO().setId(user.getId()).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        when(socialUserApi.getSocialUserByCode(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(CommonResult.success(socialUser("openid-4", user.getId())));

        assertServiceException(() -> authService.weixinMiniAppLogin(miniAppReqVO(), MINI_APP),
                AUTH_LOGIN_USER_DISABLED);
    }

    // ========== 社交登录与绑定 ==========

    @Test
    void socialLogin_已绑定时签发令牌() {
        MemberUserDO user = createUser("13922220051", "pass123");
        when(socialUserApi.getSocialUserByCode(UserTypeEnum.MEMBER.getValue(), SocialTypeEnum.WECHAT_MP.getType(),
                "code", "state")).thenReturn(CommonResult.success(socialUser("openid-mp", user.getId())));

        AppAuthLoginRespVO resp = authService.socialLogin(socialLoginReqVO(), APP);

        assertThat(resp.getUserId()).isEqualTo(user.getId());
        assertThat(resp.getOpenid()).isEqualTo("openid-mp");
    }

    @Test
    void socialLogin_未绑定应抛AUTH_SOCIAL_USER_NOT_BIND() {
        when(socialUserApi.getSocialUserByCode(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(CommonResult.success(socialUser("openid-mp", null)));

        assertServiceException(() -> authService.socialLogin(socialLoginReqVO(), APP), AUTH_SOCIAL_USER_NOT_BIND);
        verify(oauth2TokenApi, never()).createAccessToken(any());
    }

    @Test
    void socialBind_重复绑定同一社交账号应失败() {
        MemberUserDO user = createUser("13922220052", "pass123");
        when(socialUserApi.bindSocialUser(any())).thenReturn(CommonResult.error(SOCIAL_USER_BIND_DUPLICATE));
        AppAuthSocialBindReqVO reqVO = new AppAuthSocialBindReqVO();
        reqVO.setType(SocialTypeEnum.WECHAT_MP.getType());
        reqVO.setCode("code");
        reqVO.setState("state");

        assertThatThrownBy(() -> authService.socialBind(user.getId(), reqVO))
                .extracting("code").isEqualTo(SOCIAL_USER_BIND_DUPLICATE.getCode());
        ArgumentCaptor<SocialUserBindReqDTO> bind = ArgumentCaptor.forClass(SocialUserBindReqDTO.class);
        verify(socialUserApi).bindSocialUser(bind.capture());
        assertThat(bind.getValue().getUserId()).isEqualTo(user.getId());
        assertThat(bind.getValue().getUserType()).isEqualTo(UserTypeEnum.MEMBER.getValue());
    }

    // ========== 令牌 ==========

    @Test
    void refreshToken_应签发新令牌并作废旧令牌() {
        when(oauth2TokenApi.refreshAccessToken("rt-old", OAuth2ClientConstants.CLIENT_ID_H5))
                .thenReturn(CommonResult.success(token("at-new", 7L)));

        AppAuthLoginRespVO resp = authService.refreshToken("rt-old", OAuth2ClientConstants.CLIENT_ID_H5);

        assertThat(resp.getAccessToken()).isEqualTo("at-new");
        assertThat(resp.getUserId()).isEqualTo(7L);
        assertThat(resp.getUserType()).isEqualTo(UserTypeEnum.MEMBER.getValue());
        // 旧访问令牌的作废由 system 在刷新时完成，member 只透传刷新令牌与客户端
        verify(oauth2TokenApi).refreshAccessToken("rt-old", OAuth2ClientConstants.CLIENT_ID_H5);
    }

    @Test
    void refreshToken_刷新令牌属于管理员时应拒绝() {
        OAuth2AccessTokenRespDTO adminToken = token("at-admin", 1L);
        adminToken.setUserType(UserTypeEnum.ADMIN.getValue());
        when(oauth2TokenApi.refreshAccessToken(anyString(), anyString())).thenReturn(CommonResult.success(adminToken));
        when(oauth2TokenApi.removeAccessToken("at-admin")).thenReturn(CommonResult.success(adminToken));

        assertServiceException(() -> authService.refreshToken("rt-admin", OAuth2ClientConstants.CLIENT_ID_APP),
                GlobalErrorCodeConstants.UNAUTHORIZED);
        verify(oauth2TokenApi).removeAccessToken("at-admin");
    }

    @Test
    void logout_应作废令牌且幂等() {
        MemberUserDO user = createUser("13922220061", "pass123");
        when(oauth2TokenApi.removeAccessToken("at-1"))
                .thenReturn(CommonResult.success(token("at-1", user.getId())))
                .thenReturn(CommonResult.success(null));

        authService.logout("at-1", "10.0.0.1", "ua");
        authService.logout("at-1", "10.0.0.1", "ua");

        verify(oauth2TokenApi, times(2)).removeAccessToken("at-1");
        // 第二次令牌已不存在，不应再记登出日志
        ArgumentCaptor<LoginLogCreateReqDTO> captor = ArgumentCaptor.forClass(LoginLogCreateReqDTO.class);
        verify(loginLogApi, times(1)).createLoginLog(captor.capture());
        assertThat(captor.getValue().getLogType()).isEqualTo(LoginLogTypeEnum.LOGOUT_SELF.getType());
        assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
        assertThat(captor.getValue().getUsername()).isEqualTo("13922220061");
    }

    @Test
    void 不同clientId登录应各自独立_互不影响() {
        MemberUserDO user = createUser("13922220071", "pass123");

        AppAuthLoginRespVO app = authService.login(loginReqVO("13922220071", "pass123"), APP);
        AppAuthLoginRespVO mini = authService.login(loginReqVO("13922220071", "pass123"), MINI_APP);

        assertThat(app.getAccessToken()).isNotEqualTo(mini.getAccessToken());
        ArgumentCaptor<OAuth2AccessTokenCreateReqDTO> captor = ArgumentCaptor.forClass(OAuth2AccessTokenCreateReqDTO.class);
        verify(oauth2TokenApi, times(2)).createAccessToken(captor.capture());
        assertThat(captor.getAllValues()).extracting(OAuth2AccessTokenCreateReqDTO::getClientId)
                .containsExactly(OAuth2ClientConstants.CLIENT_ID_APP, OAuth2ClientConstants.CLIENT_ID_MP_WEIXIN);
        assertThat(captor.getAllValues()).allSatisfy(req -> assertThat(req.getUserId()).isEqualTo(user.getId()));
        // 登录不应作废该用户的其他令牌
        verify(oauth2TokenApi, never()).removeAccessToken(any());
        verify(oauth2TokenApi, never()).removeAccessTokensByUser(any(), any());
    }

    @Test
    void 登录日志RPC失败不应影响登录() {
        createUser("13922220072", "pass123");
        when(loginLogApi.createLoginLog(any())).thenThrow(new IllegalStateException("system 不可用"));

        assertThat(authService.login(loginReqVO("13922220072", "pass123"), APP).getAccessToken()).isNotNull();
    }

    // ========== 用户信息 ==========

    @Test
    void getPermissionInfo_返回用户与已开通的功能() {
        MemberUserDO user = createUser("13922220081", "pass123");

        AppAuthPermissionInfoRespVO info = authService.getPermissionInfo(user.getId());

        assertThat(info.getUser().getId()).isEqualTo(user.getId());
        assertThat(info.getUser().getMobile()).isEqualTo("13922220081");
        assertThat(info.getFeatures()).containsExactlyInAnyOrder("point", "sign-in");
    }

    // ========== 工具方法 ==========

    private MemberUserDO createUser(String mobile, String password) {
        MemberUserDO user = userService.createUserIfAbsent(mobile, "127.0.0.1", TerminalEnum.APP.getTerminal());
        userService.updateUserPassword(user.getId(), password);
        return user;
    }

    private OAuth2AccessTokenCreateReqDTO captureTokenRequest() {
        ArgumentCaptor<OAuth2AccessTokenCreateReqDTO> captor = ArgumentCaptor.forClass(OAuth2AccessTokenCreateReqDTO.class);
        verify(oauth2TokenApi).createAccessToken(captor.capture());
        return captor.getValue();
    }

    private SmsCodeUseReqDTO captureSmsCodeUse() {
        ArgumentCaptor<SmsCodeUseReqDTO> captor = ArgumentCaptor.forClass(SmsCodeUseReqDTO.class);
        verify(smsCodeApi).useSmsCode(captor.capture());
        return captor.getValue();
    }

    private void assertLoginLog(LoginLogTypeEnum type, LoginResultEnum result, Long userId) {
        ArgumentCaptor<LoginLogCreateReqDTO> captor = ArgumentCaptor.forClass(LoginLogCreateReqDTO.class);
        verify(loginLogApi, atLeastOnce()).createLoginLog(captor.capture());
        LoginLogCreateReqDTO log = captor.getValue();
        assertThat(log.getLogType()).isEqualTo(type.getType());
        assertThat(log.getResult()).isEqualTo(result.getResult());
        assertThat(log.getUserType()).isEqualTo(UserTypeEnum.MEMBER.getValue());
        if (userId != null) {
            assertThat(log.getUserId()).isEqualTo(userId);
        }
    }

    private static OAuth2AccessTokenRespDTO token(String accessToken, Long userId) {
        OAuth2AccessTokenRespDTO dto = new OAuth2AccessTokenRespDTO();
        dto.setAccessToken(accessToken);
        dto.setRefreshToken("rt-" + accessToken);
        dto.setUserId(userId);
        dto.setUserType(UserTypeEnum.MEMBER.getValue());
        dto.setExpiresTime(LocalDateTime.now().plusHours(1));
        return dto;
    }

    private static SocialUserRespDTO socialUser(String openid, Long userId) {
        SocialUserRespDTO dto = new SocialUserRespDTO();
        dto.setOpenid(openid);
        dto.setNickname("微信用户");
        dto.setUserId(userId);
        return dto;
    }

    private static SocialWxPhoneNumberInfoRespDTO phoneInfo(String mobile) {
        SocialWxPhoneNumberInfoRespDTO dto = new SocialWxPhoneNumberInfoRespDTO();
        dto.setPurePhoneNumber(mobile);
        dto.setPhoneNumber("+86" + mobile);
        dto.setCountryCode("86");
        return dto;
    }

    private static AppAuthLoginReqVO loginReqVO(String mobile, String password) {
        AppAuthLoginReqVO reqVO = new AppAuthLoginReqVO();
        reqVO.setMobile(mobile);
        reqVO.setPassword(password);
        return reqVO;
    }

    private static AppAuthSmsLoginReqVO smsLoginReqVO(String mobile, String code) {
        AppAuthSmsLoginReqVO reqVO = new AppAuthSmsLoginReqVO();
        reqVO.setMobile(mobile);
        reqVO.setCode(code);
        return reqVO;
    }

    private static AppAuthSmsSendReqVO smsSendReqVO(String mobile, SmsSceneEnum scene) {
        AppAuthSmsSendReqVO reqVO = new AppAuthSmsSendReqVO();
        reqVO.setMobile(mobile);
        reqVO.setScene(scene.getScene());
        return reqVO;
    }

    private static AppAuthWeixinMiniAppLoginReqVO miniAppReqVO() {
        AppAuthWeixinMiniAppLoginReqVO reqVO = new AppAuthWeixinMiniAppLoginReqVO();
        reqVO.setLoginCode("login-code");
        reqVO.setPhoneCode("phone-code");
        return reqVO;
    }

    private static AppAuthSocialLoginReqVO socialLoginReqVO() {
        AppAuthSocialLoginReqVO reqVO = new AppAuthSocialLoginReqVO();
        reqVO.setType(SocialTypeEnum.WECHAT_MP.getType());
        reqVO.setCode("code");
        reqVO.setState("state");
        return reqVO;
    }
}
