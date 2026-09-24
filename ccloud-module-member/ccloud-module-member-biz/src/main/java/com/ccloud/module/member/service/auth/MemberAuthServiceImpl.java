package com.ccloud.module.member.service.auth;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.TerminalEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.module.member.controller.app.auth.vo.*;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.dal.redis.auth.MemberLoginFailCountRedisDAO;
import com.ccloud.module.member.service.user.MemberUserService;
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
import com.ccloud.module.system.api.social.dto.SocialUserUnbindReqDTO;
import com.ccloud.module.system.api.social.dto.SocialWxPhoneNumberInfoRespDTO;
import com.ccloud.module.system.enums.logger.LoginLogTypeEnum;
import com.ccloud.module.system.enums.logger.LoginResultEnum;
import com.ccloud.module.system.enums.sms.SmsSceneEnum;
import com.ccloud.module.system.enums.social.SocialTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.ccloud.module.member.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class MemberAuthServiceImpl implements MemberAuthService {

    private static final Integer USER_TYPE = UserTypeEnum.MEMBER.getValue();

    /**
     * 小程序登录没有 OAuth2 回调，system 侧不校验 state；固定值只用于配合授权码复用的查询条件
     */
    static final String WEIXIN_MINI_APP_STATE = "mini-app";

    /**
     * 会员侧可发送的短信场景。管理后台场景由 system 自己发送
     */
    private static final Set<Integer> MEMBER_SMS_SCENES = Set.of(SmsSceneEnum.MEMBER_LOGIN.getScene(),
            SmsSceneEnum.MEMBER_UPDATE_MOBILE.getScene(), SmsSceneEnum.MEMBER_UPDATE_PASSWORD.getScene(),
            SmsSceneEnum.MEMBER_RESET_PASSWORD.getScene());

    @Resource
    private MemberUserService userService;
    @Resource
    private MemberLoginFailCountRedisDAO loginFailCountRedisDAO;
    @Resource
    private OAuth2TokenApi oauth2TokenApi;
    @Resource
    private SmsCodeApi smsCodeApi;
    @Resource
    private SocialUserApi socialUserApi;
    @Resource
    private SocialClientApi socialClientApi;
    @Resource
    private LoginLogApi loginLogApi;

    @Value("${ccloud.member.auth.login-fail-limit:5}")
    private int loginFailLimit;
    @Value("${ccloud.member.auth.login-fail-lock-minutes:10}")
    private int loginFailLockMinutes;
    /**
     * 已开通的 C 端功能，前端据此显示入口
     */
    @Value("${ccloud.member.auth.features:}")
    private Set<String> features;

    // ========== 手机号 + 密码 ==========

    @Override
    public AppAuthLoginRespVO login(AppAuthLoginReqVO reqVO, MemberLoginContext context) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String mobile = reqVO.getMobile();
        if (loginFailCountRedisDAO.get(tenantId, mobile) >= loginFailLimit) {
            createLoginLog(null, mobile, LoginLogTypeEnum.LOGIN_USERNAME, LoginResultEnum.BAD_CREDENTIALS, context);
            throw exception(AUTH_LOGIN_FAIL_LOCKED, loginFailLockMinutes);
        }

        MemberUserDO user = userService.getUserByMobile(mobile);
        // 手机号不存在与密码错误返回同一错误码，避免被用来探测已注册的手机号
        if (user == null || !userService.isPasswordMatch(reqVO.getPassword(), user.getPassword())) {
            loginFailCountRedisDAO.increment(tenantId, mobile, Duration.ofMinutes(loginFailLockMinutes));
            createLoginLog(user != null ? user.getId() : null, mobile, LoginLogTypeEnum.LOGIN_USERNAME,
                    LoginResultEnum.BAD_CREDENTIALS, context);
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        validateUserEnabled(user, LoginLogTypeEnum.LOGIN_USERNAME, context);
        loginFailCountRedisDAO.delete(tenantId, mobile);
        return createTokenAfterLoginSuccess(user, LoginLogTypeEnum.LOGIN_USERNAME, context, null);
    }

    // ========== 短信验证码 ==========

    @Override
    public void sendSmsCode(Long loginUserId, AppAuthSmsSendReqVO reqVO, String userIp) {
        Integer scene = reqVO.getScene();
        if (!MEMBER_SMS_SCENES.contains(scene)) {
            throw invalidParamException("不支持的短信场景({})", scene);
        }
        String mobile = reqVO.getMobile();
        if (SmsSceneEnum.MEMBER_UPDATE_PASSWORD.getScene().equals(scene)
                || SmsSceneEnum.MEMBER_UPDATE_MOBILE.getScene().equals(scene)) {
            if (loginUserId == null) {
                throw exception(GlobalErrorCodeConstants.UNAUTHORIZED);
            }
            // 修改密码的验证码只能发往本人手机号，否则等于允许用任意号码的验证码改密码
            if (SmsSceneEnum.MEMBER_UPDATE_PASSWORD.getScene().equals(scene)) {
                mobile = userService.validateUserExists(loginUserId).getMobile();
            }
        } else if (SmsSceneEnum.MEMBER_RESET_PASSWORD.getScene().equals(scene)
                && userService.getUserByMobile(mobile) == null) {
            throw exception(USER_MOBILE_NOT_EXISTS);
        }
        if (StrUtil.isEmpty(mobile)) {
            throw invalidParamException("手机号不能为空");
        }
        SmsCodeSendReqDTO sendReqDTO = new SmsCodeSendReqDTO();
        sendReqDTO.setMobile(mobile);
        sendReqDTO.setScene(scene);
        sendReqDTO.setCreateIp(userIp);
        smsCodeApi.sendSmsCode(sendReqDTO).checkError();
    }

    @Override
    public AppAuthLoginRespVO smsLogin(AppAuthSmsLoginReqVO reqVO, MemberLoginContext context) {
        try {
            useSmsCode(reqVO.getMobile(), reqVO.getCode(), SmsSceneEnum.MEMBER_LOGIN, context.userIp());
        } catch (ServiceException ex) {
            createLoginLog(null, reqVO.getMobile(), LoginLogTypeEnum.LOGIN_MOBILE,
                    LoginResultEnum.CAPTCHA_CODE_ERROR, context);
            throw ex;
        }
        // 验证码已证明手机号归属，未注册则直接注册
        MemberUserDO user = userService.createUserIfAbsent(reqVO.getMobile(), context.userIp(), context.terminal());
        validateUserEnabled(user, LoginLogTypeEnum.LOGIN_MOBILE, context);
        return createTokenAfterLoginSuccess(user, LoginLogTypeEnum.LOGIN_MOBILE, context, null);
    }

    @Override
    public void resetPassword(AppAuthResetPasswordReqVO reqVO, String userIp) {
        useSmsCode(reqVO.getMobile(), reqVO.getCode(), SmsSceneEnum.MEMBER_RESET_PASSWORD, userIp);
        MemberUserDO user = userService.getUserByMobile(reqVO.getMobile());
        if (user == null) {
            throw exception(USER_MOBILE_NOT_EXISTS);
        }
        userService.updateUserPassword(user.getId(), reqVO.getPassword());
        // 忘记密码往往意味着密码可能已泄露，已登录的会话一并下线
        oauth2TokenApi.removeAccessTokensByUser(user.getId(), USER_TYPE).checkError();
    }

    @Override
    public void updatePassword(Long userId, AppAuthUpdatePasswordReqVO reqVO, String userIp) {
        MemberUserDO user = userService.validateUserExists(userId);
        useSmsCode(user.getMobile(), reqVO.getCode(), SmsSceneEnum.MEMBER_UPDATE_PASSWORD, userIp);
        userService.updateUserPassword(userId, reqVO.getPassword());
        oauth2TokenApi.removeAccessTokensByUser(userId, USER_TYPE).checkError();
    }

    private void useSmsCode(String mobile, String code, SmsSceneEnum scene, String userIp) {
        SmsCodeUseReqDTO reqDTO = new SmsCodeUseReqDTO();
        reqDTO.setMobile(mobile);
        reqDTO.setCode(code);
        reqDTO.setScene(scene.getScene());
        reqDTO.setUsedIp(userIp);
        smsCodeApi.useSmsCode(reqDTO).checkError();
    }

    // ========== 社交登录 ==========

    @Override
    public String getSocialAuthorizeUrl(Integer socialType, String redirectUri) {
        return socialClientApi.getAuthorizeUrl(socialType, USER_TYPE, redirectUri).getCheckedData();
    }

    @Override
    public AppAuthLoginRespVO socialLogin(AppAuthSocialLoginReqVO reqVO, MemberLoginContext context) {
        SocialUserRespDTO socialUser = socialUserApi.getSocialUserByCode(USER_TYPE, reqVO.getType(),
                reqVO.getCode(), reqVO.getState()).getCheckedData();
        MemberUserDO user = socialUser != null && socialUser.getUserId() != null
                ? userService.getUser(socialUser.getUserId()) : null;
        if (user == null) {
            throw exception(AUTH_SOCIAL_USER_NOT_BIND);
        }
        validateUserEnabled(user, LoginLogTypeEnum.LOGIN_SOCIAL, context);
        return createTokenAfterLoginSuccess(user, LoginLogTypeEnum.LOGIN_SOCIAL, context, socialUser.getOpenid());
    }

    @Override
    public AppAuthLoginRespVO weixinMiniAppLogin(AppAuthWeixinMiniAppLoginReqVO reqVO, MemberLoginContext context) {
        Integer socialType = SocialTypeEnum.WECHAT_MINI_PROGRAM.getType();
        SocialUserRespDTO socialUser = socialUserApi.getSocialUserByCode(USER_TYPE, socialType,
                reqVO.getLoginCode(), WEIXIN_MINI_APP_STATE).getCheckedData();
        if (socialUser == null) {
            throw exception(AUTH_SOCIAL_USER_NOT_FOUND);
        }

        // 已绑定：以绑定关系为准，不再换手机号（微信按次计费；用户换了微信手机号也应登录原账号）
        MemberUserDO user = socialUser.getUserId() != null ? userService.getUser(socialUser.getUserId()) : null;
        if (user == null) {
            SocialWxPhoneNumberInfoRespDTO phone = socialClientApi.getWxMaPhoneNumberInfo(USER_TYPE,
                    reqVO.getPhoneCode()).getCheckedData();
            if (phone == null || StrUtil.isEmpty(phone.getPurePhoneNumber())) {
                throw exception(AUTH_SOCIAL_USER_NOT_FOUND);
            }
            Integer terminal = context.terminal() != null ? context.terminal()
                    : TerminalEnum.WECHAT_MINI_PROGRAM.getTerminal();
            user = userService.createUserIfAbsent(phone.getPurePhoneNumber(), context.userIp(), terminal);
            validateUserEnabled(user, LoginLogTypeEnum.LOGIN_SOCIAL, context);
            // 授权码在 system 侧短时复用，这里再次提交同一个 loginCode 不会重复向微信换取
            SocialUserBindReqDTO bindReqDTO = new SocialUserBindReqDTO();
            bindReqDTO.setUserId(user.getId());
            bindReqDTO.setUserType(USER_TYPE);
            bindReqDTO.setSocialType(socialType);
            bindReqDTO.setCode(reqVO.getLoginCode());
            bindReqDTO.setState(WEIXIN_MINI_APP_STATE);
            socialUserApi.bindSocialUser(bindReqDTO).checkError();
        } else {
            validateUserEnabled(user, LoginLogTypeEnum.LOGIN_SOCIAL, context);
        }
        return createTokenAfterLoginSuccess(user, LoginLogTypeEnum.LOGIN_SOCIAL, context, socialUser.getOpenid());
    }

    @Override
    public String socialBind(Long userId, AppAuthSocialBindReqVO reqVO) {
        userService.validateUserExists(userId);
        SocialUserBindReqDTO bindReqDTO = new SocialUserBindReqDTO();
        bindReqDTO.setUserId(userId);
        bindReqDTO.setUserType(USER_TYPE);
        bindReqDTO.setSocialType(reqVO.getType());
        bindReqDTO.setCode(reqVO.getCode());
        bindReqDTO.setState(reqVO.getState());
        return socialUserApi.bindSocialUser(bindReqDTO).getCheckedData();
    }

    @Override
    public void socialUnbind(Long userId, AppAuthSocialUnbindReqVO reqVO) {
        SocialUserUnbindReqDTO unbindReqDTO = new SocialUserUnbindReqDTO();
        unbindReqDTO.setUserId(userId);
        unbindReqDTO.setUserType(USER_TYPE);
        unbindReqDTO.setSocialType(reqVO.getType());
        unbindReqDTO.setOpenid(reqVO.getOpenid());
        socialUserApi.unbindSocialUser(unbindReqDTO).checkError();
    }

    // ========== 令牌 ==========

    @Override
    public AppAuthLoginRespVO refreshToken(String refreshToken, String clientId) {
        OAuth2AccessTokenRespDTO token = oauth2TokenApi.refreshAccessToken(refreshToken, clientId).getCheckedData();
        // 刷新令牌不区分用户体系：管理员的刷新令牌不能在 C 端换出令牌
        if (!USER_TYPE.equals(token.getUserType())) {
            oauth2TokenApi.removeAccessToken(token.getAccessToken());
            throw exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }
        return buildLoginResp(token, null);
    }

    @Override
    public void logout(String accessToken, String userIp, String userAgent) {
        OAuth2AccessTokenRespDTO token = oauth2TokenApi.removeAccessToken(accessToken).getCheckedData();
        if (token == null || !USER_TYPE.equals(token.getUserType())) {
            return;
        }
        MemberUserDO user = userService.getUser(token.getUserId());
        createLoginLog(token.getUserId(), user != null ? user.getMobile() : "", LoginLogTypeEnum.LOGOUT_SELF,
                LoginResultEnum.SUCCESS, new MemberLoginContext(userIp, userAgent, null, null));
    }

    // ========== 用户信息 ==========

    @Override
    public AppAuthPermissionInfoRespVO getPermissionInfo(Long userId) {
        MemberUserDO user = userService.validateUserExists(userId);
        AppAuthPermissionInfoRespVO respVO = new AppAuthPermissionInfoRespVO();
        respVO.setUser(BeanUtils.toBean(user, AppAuthPermissionInfoRespVO.UserVO.class));
        respVO.setFeatures(features);
        return respVO;
    }

    // ========== 公共 ==========

    private void validateUserEnabled(MemberUserDO user, LoginLogTypeEnum logType, MemberLoginContext context) {
        if (CommonStatusEnum.isDisable(user.getStatus())) {
            createLoginLog(user.getId(), user.getMobile(), logType, LoginResultEnum.USER_DISABLED, context);
            throw exception(AUTH_LOGIN_USER_DISABLED);
        }
    }

    private AppAuthLoginRespVO createTokenAfterLoginSuccess(MemberUserDO user, LoginLogTypeEnum logType,
                                                            MemberLoginContext context, String openid) {
        OAuth2AccessTokenCreateReqDTO reqDTO = new OAuth2AccessTokenCreateReqDTO();
        reqDTO.setUserId(user.getId());
        reqDTO.setUserType(USER_TYPE);
        reqDTO.setClientId(context.clientId());
        OAuth2AccessTokenRespDTO token = oauth2TokenApi.createAccessToken(reqDTO).getCheckedData();
        userService.updateUserLogin(user.getId(), context.userIp());
        createLoginLog(user.getId(), user.getMobile(), logType, LoginResultEnum.SUCCESS, context);
        return buildLoginResp(token, openid);
    }

    private static AppAuthLoginRespVO buildLoginResp(OAuth2AccessTokenRespDTO token, String openid) {
        AppAuthLoginRespVO respVO = new AppAuthLoginRespVO();
        respVO.setUserId(token.getUserId());
        respVO.setUserType(USER_TYPE);
        respVO.setAccessToken(token.getAccessToken());
        respVO.setRefreshToken(token.getRefreshToken());
        respVO.setExpiresTime(token.getExpiresTime());
        respVO.setOpenid(openid);
        return respVO;
    }

    /**
     * 登录日志经 RPC 写入 system。写入失败只记错误日志：审计通道故障不应让所有会员无法登录
     */
    private void createLoginLog(Long userId, String mobile, LoginLogTypeEnum logType, LoginResultEnum result,
                                MemberLoginContext context) {
        LoginLogCreateReqDTO reqDTO = new LoginLogCreateReqDTO();
        reqDTO.setLogType(logType.getType());
        reqDTO.setUserId(Objects.requireNonNullElse(userId, 0L));
        reqDTO.setUserType(USER_TYPE);
        reqDTO.setUsername(mobile);
        reqDTO.setResult(result.getResult());
        reqDTO.setUserIp(StrUtil.blankToDefault(context.userIp(), "unknown"));
        reqDTO.setUserAgent(StrUtil.nullToEmpty(context.userAgent()));
        try {
            loginLogApi.createLoginLog(reqDTO).checkError();
        } catch (RuntimeException ex) {
            log.error("[createLoginLog] 登录日志写入失败 userId={} type={} result={}", userId, logType, result, ex);
        }
    }
}
