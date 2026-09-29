package com.ccloud.module.system.controller.admin.auth;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthRegisterReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthResetPasswordReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthSmsLoginReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthSmsSendReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthSocialBindLoginReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthSocialLoginReqVO;
import com.ccloud.module.system.enums.logger.LoginLogTypeEnum;
import com.ccloud.module.system.service.auth.AdminAuthService;
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

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 认证")
@RestController
@RequestMapping("/admin-api/system/auth")
@Validated
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    @Resource
    private AdminAuthService authService;

    @PostMapping("/login")
    @Operation(summary = "账号密码登录")
    public CommonResult<AuthLoginRespVO> login(@RequestBody @Valid AuthLoginReqVO reqVO, HttpServletRequest request) {
        return success(authService.login(reqVO, ServletUtils.getClientIP(request), ServletUtils.getUserAgent(request)));
    }

    @PostMapping("/send-sms-code")
    @Operation(summary = "发送短信验证码", description = "用于短信登录与忘记密码")
    public CommonResult<Boolean> sendSmsCode(@RequestBody @Valid AuthSmsSendReqVO reqVO, HttpServletRequest request) {
        authService.sendSmsCode(reqVO, ServletUtils.getClientIP(request));
        return success(true);
    }

    @PostMapping("/sms-login")
    @Operation(summary = "短信验证码登录")
    public CommonResult<AuthLoginRespVO> smsLogin(@RequestBody @Valid AuthSmsLoginReqVO reqVO,
                                                  HttpServletRequest request) {
        return success(authService.smsLogin(reqVO, ServletUtils.getClientIP(request),
                ServletUtils.getUserAgent(request)));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "短信验证码重置密码", description = "重置后该账号已登录的会话全部下线")
    public CommonResult<Boolean> resetPassword(@RequestBody @Valid AuthResetPasswordReqVO reqVO,
                                               HttpServletRequest request) {
        authService.resetPassword(reqVO, ServletUtils.getClientIP(request));
        return success(true);
    }

    @GetMapping("/social-auth-redirect")
    @Operation(summary = "获得社交登录的授权跳转地址")
    @Parameters({
            @Parameter(name = "type", description = "社交平台，见 SocialTypeEnum", required = true, example = "20"),
            @Parameter(name = "redirectUri", description = "授权回调地址", required = true,
                    example = "https://admin.ccloud.local/social-login")
    })
    public CommonResult<String> socialAuthRedirect(@RequestParam("type") Integer type,
                                                   @RequestParam("redirectUri") String redirectUri) {
        return success(authService.getSocialAuthorizeUrl(type, redirectUri));
    }

    @PostMapping("/social-login")
    @Operation(summary = "社交登录", description = "社交账号未绑定时返回 AUTH_THIRD_LOGIN_NOT_BIND，前端据此引导绑定")
    public CommonResult<AuthLoginRespVO> socialLogin(@RequestBody @Valid AuthSocialLoginReqVO reqVO,
                                                     HttpServletRequest request) {
        return success(authService.socialLogin(reqVO, ServletUtils.getClientIP(request),
                ServletUtils.getUserAgent(request)));
    }

    @PostMapping("/social-bind-login")
    @Operation(summary = "社交登录并绑定已有账号")
    public CommonResult<AuthLoginRespVO> socialBindLogin(@RequestBody @Valid AuthSocialBindLoginReqVO reqVO,
                                                         HttpServletRequest request) {
        return success(authService.socialBindLogin(reqVO, ServletUtils.getClientIP(request),
                ServletUtils.getUserAgent(request)));
    }

    @PostMapping("/register")
    @Operation(summary = "注册并登录", description = "是否开放注册由服务配置决定")
    public CommonResult<AuthLoginRespVO> register(@RequestBody @Valid AuthRegisterReqVO reqVO,
                                                  HttpServletRequest request) {
        return success(authService.register(reqVO, ServletUtils.getClientIP(request),
                ServletUtils.getUserAgent(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出", description = "令牌无效时同样返回成功，保证幂等")
    public CommonResult<Boolean> logout(HttpServletRequest request) {
        String token = obtainAccessToken(request);
        if (token != null) {
            authService.logout(token, LoginLogTypeEnum.LOGOUT_SELF.getType());
        }
        return success(true);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "刷新令牌")
    @Parameter(name = "refreshToken", description = "刷新令牌", required = true,
            example = "0a1b2c3d4e5f67890a1b2c3d4e5f6789")
    public CommonResult<AuthLoginRespVO> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        return success(authService.refreshToken(refreshToken));
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

    @GetMapping("/get-permission-info")
    @Operation(summary = "获得登录用户的权限信息", description = "前端据此渲染菜单与按钮")
    public CommonResult<AuthPermissionInfoRespVO> getPermissionInfo() {
        return success(authService.getPermissionInfo(SecurityFrameworkUtils.getLoginUserId()));
    }
}
