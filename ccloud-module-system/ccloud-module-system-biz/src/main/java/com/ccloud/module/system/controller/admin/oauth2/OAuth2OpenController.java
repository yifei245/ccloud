package com.ccloud.module.system.controller.admin.oauth2;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.system.controller.admin.oauth2.vo.open.OAuth2OpenAccessTokenRespVO;
import com.ccloud.module.system.controller.admin.oauth2.vo.open.OAuth2OpenAuthorizeInfoRespVO;
import com.ccloud.module.system.controller.admin.oauth2.vo.open.OAuth2OpenCheckTokenRespVO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2ApproveDO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import com.ccloud.module.system.enums.oauth2.OAuth2GrantTypeEnum;
import com.ccloud.module.system.service.oauth2.OAuth2ApproveService;
import com.ccloud.module.system.service.oauth2.OAuth2ClientService;
import com.ccloud.module.system.service.oauth2.OAuth2AuthorizeService;
import com.ccloud.module.system.service.oauth2.OAuth2GrantService;
import com.ccloud.module.system.service.oauth2.OAuth2TokenService;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * OAuth2 标准端点：令牌签发、撤销、校验与授权码模式的授权页。
 *
 * <p>令牌端点与校验端点免登录，改为校验客户端凭据（HTTP Basic 或 client_id/client_secret 参数）；
 * 授权端点需要用户已登录管理后台。
 */
@Tag(name = "管理后台 - OAuth2 开放端点")
@RestController
@RequestMapping("/admin-api/system/oauth2")
@Validated
public class OAuth2OpenController {

    @Resource
    private OAuth2GrantService oauth2GrantService;
    @Resource
    private OAuth2ClientService oauth2ClientService;
    @Resource
    private OAuth2ApproveService oauth2ApproveService;
    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private OAuth2AuthorizeService oauth2AuthorizeService;

    @PostMapping("/token")
    @Operation(summary = "获得访问令牌", description = "支持 authorization_code、password、refresh_token、client_credentials")
    @Parameters({
            @Parameter(name = "grant_type", required = true, description = "授权类型", example = "authorization_code"),
            @Parameter(name = "code", description = "授权码", example = "0a1b2c3d4e5f67890a1b2c3d4e5f6789"),
            @Parameter(name = "redirect_uri", description = "重定向地址，须与申请授权码时一致", example = "http://localhost:8080/cb"),
            @Parameter(name = "state", description = "状态", example = "1"),
            @Parameter(name = "username", example = "admin"),
            @Parameter(name = "password", example = "admin123"),
            @Parameter(name = "scope", description = "授权范围，空格分隔", example = "user.read"),
            @Parameter(name = "refresh_token", example = "0a1b2c3d4e5f67890a1b2c3d4e5f6789")
    })
    public CommonResult<OAuth2OpenAccessTokenRespVO> postAccessToken(HttpServletRequest request,
                                                                     @RequestParam("grant_type") String grantType,
                                                                     @RequestParam(value = "code", required = false) String code,
                                                                     @RequestParam(value = "redirect_uri", required = false) String redirectUri,
                                                                     @RequestParam(value = "state", required = false) String state,
                                                                     @RequestParam(value = "username", required = false) String username,
                                                                     @RequestParam(value = "password", required = false) String password,
                                                                     @RequestParam(value = "scope", required = false) String scope,
                                                                     @RequestParam(value = "refresh_token", required = false) String refreshToken) {
        OAuth2GrantTypeEnum grantTypeEnum = Arrays.stream(OAuth2GrantTypeEnum.values())
                .filter(type -> type.getGrantType().equals(grantType)).findFirst()
                .orElseThrow(() -> invalidParamException("未知授权类型({})", grantType));
        List<String> scopes = parseScopes(scope);
        String[] clientCredentials = obtainClientCredentials(request);
        OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(clientCredentials[0], clientCredentials[1],
                grantType, scopes, redirectUri);

        OAuth2AccessTokenDO accessToken = switch (grantTypeEnum) {
            case AUTHORIZATION_CODE -> oauth2GrantService.grantAuthorizationCode(client.getClientId(), code, redirectUri, state);
            case PASSWORD -> oauth2GrantService.grantPassword(username, password, client.getClientId(), scopes);
            case REFRESH_TOKEN -> oauth2GrantService.grantRefreshToken(refreshToken, client.getClientId());
            case CLIENT_CREDENTIALS -> oauth2GrantService.grantClientCredentials(client.getClientId(), scopes);
            default -> throw invalidParamException("授权类型({})请使用对应的登录接口", grantType);
        };
        return success(new OAuth2OpenAccessTokenRespVO(accessToken.getAccessToken(), accessToken.getRefreshToken(),
                "bearer", Duration.between(LocalDateTime.now(), accessToken.getExpiresTime()).getSeconds(),
                String.join(" ", CollUtil.emptyIfNull(accessToken.getScopes()))));
    }

    @DeleteMapping("/token")
    @Operation(summary = "撤销访问令牌", description = "只能撤销本客户端签发的令牌")
    @Parameter(name = "token", required = true, description = "访问令牌", example = "e1b0a9f3c6d54ef1a6b0a1c2d3e4f5a6")
    public CommonResult<Boolean> revokeToken(HttpServletRequest request, @RequestParam("token") String token) {
        String[] clientCredentials = obtainClientCredentials(request);
        OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(clientCredentials[0], clientCredentials[1],
                null, null, null);
        OAuth2AccessTokenDO accessToken = oauth2TokenService.getAccessToken(token);
        if (accessToken == null || !client.getClientId().equals(accessToken.getClientId())) {
            return success(false);
        }
        return success(oauth2TokenService.removeAccessToken(token) != null);
    }

    @PostMapping("/check-token")
    @Operation(summary = "校验访问令牌")
    @Parameter(name = "token", required = true, description = "访问令牌", example = "e1b0a9f3c6d54ef1a6b0a1c2d3e4f5a6")
    public CommonResult<OAuth2OpenCheckTokenRespVO> checkToken(HttpServletRequest request, @RequestParam("token") String token) {
        String[] clientCredentials = obtainClientCredentials(request);
        oauth2ClientService.validOAuthClientFromCache(clientCredentials[0], clientCredentials[1], null, null, null);

        OAuth2AccessTokenDO accessToken = oauth2TokenService.checkAccessToken(token);
        OAuth2OpenCheckTokenRespVO respVO = new OAuth2OpenCheckTokenRespVO();
        respVO.setUserId(accessToken.getUserId());
        respVO.setUserType(accessToken.getUserType());
        respVO.setTenantId(accessToken.getTenantId());
        respVO.setClientId(accessToken.getClientId());
        respVO.setScopes(accessToken.getScopes());
        respVO.setAccessToken(accessToken.getAccessToken());
        respVO.setExp(accessToken.getExpiresTime().atZone(ZoneId.systemDefault()).toEpochSecond());
        return success(respVO);
    }

    @GetMapping("/authorize")
    @Operation(summary = "获得授权页信息", description = "授权码模式的授权页展示客户端与申请的授权范围")
    @Parameter(name = "clientId", required = true, description = "客户端编号", example = "ccloud-web")
    public CommonResult<OAuth2OpenAuthorizeInfoRespVO> authorize(@RequestParam("clientId") String clientId) {
        OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(clientId);
        Set<String> approvedScopes = CollectionUtils.convertSet(CollectionUtils.filterList(
                oauth2ApproveService.getApproveList(getLoginUserId(), UserTypeEnum.ADMIN.getValue(), clientId),
                OAuth2ApproveDO::getApproved), OAuth2ApproveDO::getScope);
        List<OAuth2OpenAuthorizeInfoRespVO.Scope> scopes = CollectionUtils.convertList(client.getScopes(),
                scope -> new OAuth2OpenAuthorizeInfoRespVO.Scope(scope, approvedScopes.contains(scope)));
        return success(new OAuth2OpenAuthorizeInfoRespVO(
                new OAuth2OpenAuthorizeInfoRespVO.Client(client.getName(), client.getLogo()), scopes));
    }

    /**
     * 用户在授权页确认（或静默自动授权）后签发授权码，返回应跳转的地址。只支持 response_type=code
     *
     * @return 自动授权未通过（需用户确认）时返回 null，前端据此展示授权页
     */
    @PostMapping("/authorize")
    @Operation(summary = "申请授权", description = "适合授权码模式；返回携带授权码的重定向地址")
    @Parameters({
            @Parameter(name = "response_type", required = true, description = "响应类型，仅支持 code", example = "code"),
            @Parameter(name = "client_id", required = true, description = "客户端编号", example = "ccloud-web"),
            @Parameter(name = "scope", description = "授权范围及是否同意，JSON 对象", example = "{\"user.read\": true}"),
            @Parameter(name = "redirect_uri", required = true, description = "重定向地址", example = "http://localhost:8080/cb"),
            @Parameter(name = "auto_approve", required = true, description = "是否尝试自动授权", example = "true"),
            @Parameter(name = "state", example = "1")
    })
    public CommonResult<String> approveOrDeny(@RequestParam("response_type") String responseType,
                                              @RequestParam("client_id") String clientId,
                                              @RequestParam(value = "scope", required = false) String scope,
                                              @RequestParam("redirect_uri") String redirectUri,
                                              @RequestParam(value = "auto_approve") Boolean autoApprove,
                                              @RequestParam(value = "state", required = false) String state) {
        Map<String, Boolean> scopes = parseScopeApprovals(scope);
        return success(oauth2AuthorizeService.authorize(getLoginUserId(), UserTypeEnum.ADMIN.getValue(), responseType,
                clientId, scopes, redirectUri, state, Boolean.TRUE.equals(autoApprove)));
    }

    private static Long getLoginUserId() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw exception(401, "账号未登录");
        }
        return userId;
    }

    private static List<String> parseScopes(String scope) {
        return StrUtil.isBlank(scope) ? Collections.emptyList() : StrUtil.splitTrim(scope, ' ');
    }

    /**
     * 授权页提交的范围及是否同意，JSON 对象，如 {"user.read": true}
     */
    public static Map<String, Boolean> parseScopeApprovals(String scope) {
        Map<String, Boolean> scopes = StrUtil.isEmpty(scope) ? Collections.emptyMap()
                : JsonUtils.parseObject(scope, new TypeReference<LinkedHashMap<String, Boolean>>() {
        });
        if (scopes == null) {
            throw invalidParamException("scope 格式不正确");
        }
        return scopes;
    }

    /**
     * 客户端凭据：优先取 HTTP Basic 认证头（RFC 6749 2.3.1 推荐），其次取 client_id / client_secret 参数
     *
     * @return [clientId, clientSecret]
     */
    static String[] obtainClientCredentials(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StrUtil.startWithIgnoreCase(authorization, "Basic ")) {
            String decoded = new String(Base64.decode(authorization.substring(6).trim()), StandardCharsets.UTF_8);
            int index = decoded.indexOf(':');
            if (index > 0 && index < decoded.length() - 1) {
                return new String[]{decoded.substring(0, index), decoded.substring(index + 1)};
            }
        }
        String clientId = request.getParameter("client_id");
        String clientSecret = request.getParameter("client_secret");
        if (StrUtil.isEmpty(clientId) || StrUtil.isEmpty(clientSecret)) {
            throw exception(401, "客户端凭据缺失，请通过 HTTP Basic 或 client_id/client_secret 提供");
        }
        return new String[]{clientId, clientSecret};
    }
}
