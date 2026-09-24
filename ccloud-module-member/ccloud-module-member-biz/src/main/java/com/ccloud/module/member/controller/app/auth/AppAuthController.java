package com.ccloud.module.member.controller.app.auth;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.TerminalEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.member.controller.app.auth.vo.*;
import com.ccloud.module.member.service.auth.MemberAuthService;
import com.ccloud.module.member.service.auth.MemberLoginContext;
import com.ccloud.module.system.enums.oauth2.OAuth2ClientConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Set;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 认证")
@RestController
@RequestMapping("/app-api/member/auth")
@Validated
public class AppAuthController {

    /**
     * 请求头：客户端终端，取值见 {@link TerminalEnum}，注册时记录
     */
    public static final String HEADER_TERMINAL = "X-Terminal";

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * C 端可用的客户端。管理后台客户端签发的会员令牌没有意义，且会混淆按客户端的会话管理
     */
    private static final Set<String> MEMBER_CLIENT_IDS = Set.of(OAuth2ClientConstants.CLIENT_ID_APP,
            OAuth2ClientConstants.CLIENT_ID_MP_WEIXIN, OAuth2ClientConstants.CLIENT_ID_H5);

    @Resource
    private MemberAuthService authService;

    @PostMapping("/login")
    @Operation(summary = "手机号 + 密码登录")
    public CommonResult<AppAuthLoginRespVO> login(@RequestBody @Valid AppAuthLoginReqVO reqVO,
                                                  HttpServletRequest request) {
        return success(authService.login(reqVO, buildContext(request)));
    }

    @PostMapping("/sms-login")
    @Operation(summary = "手机号 + 验证码登录", description = "手机号未注册时自动注册")
    public CommonResult<AppAuthLoginRespVO> smsLogin(@RequestBody @Valid AppAuthSmsLoginReqVO reqVO,
                                                     HttpServletRequest request) {
        return success(authService.smsLogin(reqVO, buildContext(request)));
    }

    @PostMapping("/send-sms-code")
    @Operation(summary = "发送短信验证码", description = "修改密码、修改手机号场景需要登录")
    public CommonResult<Boolean> sendSmsCode(@RequestBody @Valid AppAuthSmsSendReqVO reqVO,
                                             HttpServletRequest request) {
        authService.sendSmsCode(SecurityFrameworkUtils.getLoginUserId(), reqVO, ServletUtils.getClientIP(request));
        return success(true);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "验证码重置密码", description = "重置后该用户已登录的会话全部下线")
    public CommonResult<Boolean> resetPassword(@RequestBody @Valid AppAuthResetPasswordReqVO reqVO,
                                               HttpServletRequest request) {
        authService.resetPassword(reqVO, ServletUtils.getClientIP(request));
        return success(true);
    }

    @PostMapping("/update-password")
    @Operation(summary = "修改密码", description = "验证码发往当前用户的手机号；修改后已登录的会话全部下线")
    public CommonResult<Boolean> updatePassword(@RequestBody @Valid AppAuthUpdatePasswordReqVO reqVO,
                                                HttpServletRequest request) {
        authService.updatePassword(SecurityFrameworkUtils.getLoginUserId(), reqVO, ServletUtils.getClientIP(request));
        return success(true);
    }

    @GetMapping("/social-auth-redirect")
    @Operation(summary = "获得社交登录的授权跳转地址")
    @Parameters({
            @Parameter(name = "type", description = "社交平台，见 SocialTypeEnum", required = true, example = "31"),
            @Parameter(name = "redirectUri", description = "授权回调地址", required = true,
                    example = "https://h5.ccloud.local/social-login")
    })
    public CommonResult<String> socialAuthRedirect(@RequestParam("type") Integer type,
                                                   @RequestParam("redirectUri") String redirectUri) {
        return success(authService.getSocialAuthorizeUrl(type, redirectUri));
    }

    @PostMapping("/social-login")
    @Operation(summary = "社交登录", description = "仅适用于已绑定的社交账号；未绑定返回 AUTH_SOCIAL_USER_NOT_BIND")
    public CommonResult<AppAuthLoginRespVO> socialLogin(@RequestBody @Valid AppAuthSocialLoginReqVO reqVO,
                                                        HttpServletRequest request) {
        return success(authService.socialLogin(reqVO, buildContext(request)));
    }

    @PostMapping("/weixin-mini-app-login")
    @Operation(summary = "微信小程序一键登录", description = "用 phoneCode 换取手机号，未注册时自动注册并绑定 openid")
    public CommonResult<AppAuthLoginRespVO> weixinMiniAppLogin(
            @RequestBody @Valid AppAuthWeixinMiniAppLoginReqVO reqVO, HttpServletRequest request) {
        return success(authService.weixinMiniAppLogin(reqVO, buildContext(request)));
    }

    @PostMapping("/social-bind")
    @Operation(summary = "绑定社交账号", description = "返回社交平台 openid")
    public CommonResult<String> socialBind(@RequestBody @Valid AppAuthSocialBindReqVO reqVO) {
        return success(authService.socialBind(SecurityFrameworkUtils.getLoginUserId(), reqVO));
    }

    @PostMapping("/social-unbind")
    @Operation(summary = "解绑社交账号")
    public CommonResult<Boolean> socialUnbind(@RequestBody @Valid AppAuthSocialUnbindReqVO reqVO) {
        authService.socialUnbind(SecurityFrameworkUtils.getLoginUserId(), reqVO);
        return success(true);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "刷新令牌")
    @Parameter(name = "refreshToken", description = "刷新令牌", required = true,
            example = "0a1b2c3d4e5f67890a1b2c3d4e5f6789")
    public CommonResult<AppAuthLoginRespVO> refreshToken(@RequestParam("refreshToken") String refreshToken,
                                                         HttpServletRequest request) {
        return success(authService.refreshToken(refreshToken, resolveClientId(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出", description = "令牌无效时同样返回成功，保证幂等")
    public CommonResult<Boolean> logout(HttpServletRequest request) {
        String token = obtainAccessToken(request);
        if (token != null) {
            authService.logout(token, ServletUtils.getClientIP(request), ServletUtils.getUserAgent(request));
        }
        return success(true);
    }

    @GetMapping("/get-permission-info")
    @Operation(summary = "获得登录用户信息与已开通的功能")
    public CommonResult<AppAuthPermissionInfoRespVO> getPermissionInfo() {
        return success(authService.getPermissionInfo(SecurityFrameworkUtils.getLoginUserId()));
    }

    // ========== 请求头解析 ==========

    private static MemberLoginContext buildContext(HttpServletRequest request) {
        return new MemberLoginContext(ServletUtils.getClientIP(request), ServletUtils.getUserAgent(request),
                resolveTerminal(request), resolveClientId(request));
    }

    static String resolveClientId(HttpServletRequest request) {
        String clientId = request.getHeader(OAuth2ClientConstants.HEADER_CLIENT_ID);
        if (StrUtil.isBlank(clientId)) {
            return OAuth2ClientConstants.CLIENT_ID_DEFAULT_MEMBER;
        }
        if (!MEMBER_CLIENT_IDS.contains(clientId)) {
            throw invalidParamException("不支持的客户端({})", clientId);
        }
        return clientId;
    }

    /**
     * 终端只用于统计，取值不合法时记为未知而不是拒绝登录
     */
    static Integer resolveTerminal(HttpServletRequest request) {
        String value = request.getHeader(HEADER_TERMINAL);
        if (StrUtil.isBlank(value) || !StrUtil.isNumeric(value.trim())) {
            return null;
        }
        int terminal = Integer.parseInt(value.trim());
        return Arrays.stream(TerminalEnum.values()).anyMatch(e -> e.getTerminal() == terminal) ? terminal : null;
    }

    /**
     * 登出要作废具体的那个令牌，因此需要原始令牌串。网关校验令牌后会把 Authorization 原样转发
     */
    private static String obtainAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StrUtil.startWithIgnoreCase(authorization, BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return StrUtil.isNotEmpty(token) ? token : null;
    }
}
