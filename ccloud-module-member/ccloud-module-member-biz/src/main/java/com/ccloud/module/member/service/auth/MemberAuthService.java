package com.ccloud.module.member.service.auth;

import com.ccloud.module.member.controller.app.auth.vo.*;

/**
 * 会员认证。身份校验在 member 完成，令牌统一由 system 签发（userType=MEMBER），
 * 这样统一登出、强制下线对两套用户体系同样生效
 */
public interface MemberAuthService {

    AppAuthLoginRespVO login(AppAuthLoginReqVO reqVO, MemberLoginContext context);

    /**
     * @param loginUserId 当前登录用户，未登录为 null。修改手机号、修改密码场景要求已登录
     */
    void sendSmsCode(Long loginUserId, AppAuthSmsSendReqVO reqVO, String userIp);

    AppAuthLoginRespVO smsLogin(AppAuthSmsLoginReqVO reqVO, MemberLoginContext context);

    void resetPassword(AppAuthResetPasswordReqVO reqVO, String userIp);

    void updatePassword(Long userId, AppAuthUpdatePasswordReqVO reqVO, String userIp);

    String getSocialAuthorizeUrl(Integer socialType, String redirectUri);

    AppAuthLoginRespVO socialLogin(AppAuthSocialLoginReqVO reqVO, MemberLoginContext context);

    AppAuthLoginRespVO weixinMiniAppLogin(AppAuthWeixinMiniAppLoginReqVO reqVO, MemberLoginContext context);

    /**
     * @return 绑定的社交平台 openid
     */
    String socialBind(Long userId, AppAuthSocialBindReqVO reqVO);

    void socialUnbind(Long userId, AppAuthSocialUnbindReqVO reqVO);

    AppAuthLoginRespVO refreshToken(String refreshToken, String clientId);

    /**
     * 令牌不存在或已失效时静默返回，保证幂等
     */
    void logout(String accessToken, String userIp, String userAgent);

    AppAuthPermissionInfoRespVO getPermissionInfo(Long userId);
}
