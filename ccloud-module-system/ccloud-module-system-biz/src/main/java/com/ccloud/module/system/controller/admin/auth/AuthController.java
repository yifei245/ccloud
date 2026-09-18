package com.ccloud.module.system.controller.admin.auth;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthRegisterReqVO;
import com.ccloud.module.system.enums.logger.LoginLogTypeEnum;
import com.ccloud.module.system.service.auth.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
