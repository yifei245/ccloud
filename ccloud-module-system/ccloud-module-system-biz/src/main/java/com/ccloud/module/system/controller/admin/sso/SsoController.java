package com.ccloud.module.system.controller.admin.sso;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.system.controller.admin.oauth2.vo.open.OAuth2OpenAuthorizeInfoRespVO;
import com.ccloud.module.system.controller.admin.sso.vo.SsoAuthorizeRespVO;
import com.ccloud.module.system.controller.admin.sso.vo.SsoLogoutRespVO;
import com.ccloud.module.system.dal.dataobject.logger.LoginLogDO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2ApproveDO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.enums.logger.LoginLogTypeEnum;
import com.ccloud.module.system.enums.logger.LoginResultEnum;
import com.ccloud.module.system.service.logger.LoginLogService;
import com.ccloud.module.system.service.oauth2.OAuth2ApproveService;
import com.ccloud.module.system.service.oauth2.OAuth2AuthorizeService;
import com.ccloud.module.system.service.oauth2.OAuth2ClientService;
import com.ccloud.module.system.service.oauth2.OAuth2TokenService;
import com.ccloud.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.pojo.CommonResult.success;
import static com.ccloud.module.system.controller.admin.oauth2.OAuth2OpenController.parseScopeApprovals;

/**
 * SSO 单点登录：接入方把用户带到统一登录页，登录页据授权检查结果决定登录、授权或直接跳回。
 * 授权码换令牌、校验令牌仍走 OAuth2 开放端点 {@code /admin-api/system/oauth2/*}。
 *
 * <p>授权检查与统一登出免登录：未登录时分别返回 needLogin 与幂等成功。
 * 两者都先校验重定向地址，登录页登录后不会把用户带到未登记的地址（开放重定向）。
 */
@Tag(name = "管理后台 - SSO 单点登录")
@RestController
@RequestMapping("/admin-api/system/sso")
@Validated
public class SsoController {

    @Resource
    private OAuth2AuthorizeService oauth2AuthorizeService;
    @Resource
    private OAuth2ApproveService oauth2ApproveService;
    @Resource
    private OAuth2ClientService oauth2ClientService;
    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private AdminUserService adminUserService;
    @Resource
    private LoginLogService loginLogService;

    @GetMapping("/authorize")
    @Operation(summary = "授权检查", description = "未登录返回 needLogin；可自动授权时直接返回携带授权码的跳转地址；否则返回待确认的授权范围")
    @Parameters({
            @Parameter(name = "response_type", required = true, description = "响应类型，仅支持 code", example = "code"),
            @Parameter(name = "client_id", required = true, description = "客户端编号", example = "ccloud-web"),
            @Parameter(name = "redirect_uri", required = true, description = "重定向地址", example = "http://localhost:8080/cb"),
            @Parameter(name = "scope", description = "授权范围，空格分隔", example = "user.read"),
            @Parameter(name = "state", example = "1")
    })
    public CommonResult<SsoAuthorizeRespVO> authorize(@RequestParam("response_type") String responseType,
                                                      @RequestParam("client_id") String clientId,
                                                      @RequestParam("redirect_uri") String redirectUri,
                                                      @RequestParam(value = "scope", required = false) String scope,
                                                      @RequestParam(value = "state", required = false) String state) {
        List<String> scopes = StrUtil.isBlank(scope) ? Collections.emptyList() : StrUtil.splitTrim(scope, ' ');
        OAuth2ClientDO client = oauth2AuthorizeService.validateAuthorizeRequest(responseType, clientId, scopes, redirectUri);
        SsoAuthorizeRespVO respVO = new SsoAuthorizeRespVO()
                .setClient(new OAuth2OpenAuthorizeInfoRespVO.Client(client.getName(), client.getLogo()));
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return success(respVO.setNeedLogin(true));
        }
        respVO.setNeedLogin(false);

        Integer userType = UserTypeEnum.ADMIN.getValue();
        Map<String, Boolean> requested = new LinkedHashMap<>();
        scopes.forEach(key -> requested.put(key, true));
        String redirect = oauth2AuthorizeService.authorize(userId, userType, responseType, clientId, requested,
                redirectUri, state, true);
        if (redirect != null) {
            return success(respVO.setRedirectUri(redirect));
        }
        // 需用户确认：自动授权范围与此前同意过的范围默认勾选
        Set<String> approved = new HashSet<>(client.getAutoApproveScopes());
        approved.addAll(CollectionUtils.convertSet(CollectionUtils.filterList(
                oauth2ApproveService.getApproveList(userId, userType, clientId),
                approve -> Boolean.TRUE.equals(approve.getApproved())), OAuth2ApproveDO::getScope));
        return success(respVO.setScopes(CollectionUtils.convertList(scopes,
                key -> new OAuth2OpenAuthorizeInfoRespVO.Scope(key, approved.contains(key)))));
    }

    @PostMapping("/authorize")
    @Operation(summary = "确认授权", description = "保存用户对各授权范围的选择，返回携带授权码（或 access_denied）的跳转地址")
    @Parameters({
            @Parameter(name = "response_type", required = true, description = "响应类型，仅支持 code", example = "code"),
            @Parameter(name = "client_id", required = true, description = "客户端编号", example = "ccloud-web"),
            @Parameter(name = "scope", description = "授权范围及是否同意，JSON 对象", example = "{\"user.read\": true}"),
            @Parameter(name = "redirect_uri", required = true, description = "重定向地址", example = "http://localhost:8080/cb"),
            @Parameter(name = "state", example = "1")
    })
    public CommonResult<String> approve(@RequestParam("response_type") String responseType,
                                        @RequestParam("client_id") String clientId,
                                        @RequestParam(value = "scope", required = false) String scope,
                                        @RequestParam("redirect_uri") String redirectUri,
                                        @RequestParam(value = "state", required = false) String state) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw exception(401, "账号未登录");
        }
        return success(oauth2AuthorizeService.authorize(userId, UserTypeEnum.ADMIN.getValue(), responseType, clientId,
                parseScopeApprovals(scope), redirectUri, state, false));
    }

    @GetMapping("/logout")
    @Operation(summary = "统一登出", description = "作废当前用户在全部客户端的令牌（含管理后台自身），返回其授权过的客户端")
    @Parameters({
            @Parameter(name = "client_id", required = true, description = "发起登出的客户端编号", example = "ccloud-web"),
            @Parameter(name = "redirect_uri", required = true, description = "登出后跳转地址", example = "http://localhost:8080/cb")
    })
    public CommonResult<SsoLogoutRespVO> logout(@RequestParam("client_id") String clientId,
                                                @RequestParam("redirect_uri") String redirectUri) {
        oauth2ClientService.validOAuthClientFromCache(clientId, null, null, null, redirectUri);
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return success(new SsoLogoutRespVO(redirectUri, Collections.emptySet()));
        }
        Integer userType = UserTypeEnum.ADMIN.getValue();
        Set<String> clientIds = oauth2ApproveService.getApprovedClientIds(userId, userType);
        oauth2TokenService.removeAccessTokensByUser(userId, userType);

        AdminUserDO user = adminUserService.getUser(userId);
        loginLogService.createLoginLog(new LoginLogDO().setLogType(LoginLogTypeEnum.LOGOUT_SELF.getType())
                .setUserId(userId).setUserType(userType).setUsername(user != null ? user.getUsername() : "")
                .setResult(LoginResultEnum.SUCCESS.getResult()).setUserIp("").setUserAgent(""));
        return success(new SsoLogoutRespVO(redirectUri, clientIds));
    }
}
