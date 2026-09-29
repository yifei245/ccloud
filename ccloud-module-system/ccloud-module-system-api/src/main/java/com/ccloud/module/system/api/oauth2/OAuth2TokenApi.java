package com.ccloud.module.system.api.oauth2;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.ccloud.module.system.api.oauth2.dto.OAuth2AccessTokenCheckRespDTO;
import com.ccloud.module.system.api.oauth2.dto.OAuth2AccessTokenCreateReqDTO;
import com.ccloud.module.system.api.oauth2.dto.OAuth2AccessTokenRespDTO;
import jakarta.validation.Valid;

/**
 * OAuth2 令牌 RPC。
 *
 * <p>令牌只由 system 模块签发与校验：member 等模块完成各自的身份认证后调用 {@link #createAccessToken}，
 * 网关调用 {@link #checkAccessToken} 解析身份。令牌逻辑集中一处，才能保证统一登出、强制下线全局生效。
 */
@FeignClient(name = ApiConstants.NAME, contextId = "systemOAuth2TokenApi")
@Tag(name = "RPC 服务 - OAuth2 令牌")
public interface OAuth2TokenApi {

    String PREFIX = ApiConstants.PREFIX + "/oauth2/token";

    @PostMapping(PREFIX + "/create")
    @Operation(summary = "创建访问令牌")
    CommonResult<OAuth2AccessTokenRespDTO> createAccessToken(@Valid @RequestBody OAuth2AccessTokenCreateReqDTO reqDTO);

    /**
     * 校验访问令牌。网关对每个非白名单请求调用，路径固定为 /rpc-api/system/oauth2/token/check。
     * 令牌无效或过期时返回 401 错误码而非抛异常，网关据此直接响应未登录。
     */
    @GetMapping(PREFIX + "/check")
    @Operation(summary = "校验访问令牌")
    CommonResult<OAuth2AccessTokenCheckRespDTO> checkAccessToken(@RequestParam("accessToken") String accessToken);

    @DeleteMapping(PREFIX + "/remove")
    @Operation(summary = "移除访问令牌")
    CommonResult<OAuth2AccessTokenRespDTO> removeAccessToken(@RequestParam("accessToken") String accessToken);

    /**
     * 作废用户在所有客户端的令牌。改密码、重置密码、禁用账号后调用，否则泄露的令牌在过期前仍然有效
     */
    @DeleteMapping(PREFIX + "/remove-by-user")
    @Operation(summary = "移除用户的全部访问令牌")
    CommonResult<Boolean> removeAccessTokensByUser(@RequestParam("userId") Long userId,
                                                   @RequestParam("userType") Integer userType);

    @PutMapping(PREFIX + "/refresh")
    @Operation(summary = "刷新访问令牌")
    CommonResult<OAuth2AccessTokenRespDTO> refreshAccessToken(@RequestParam("refreshToken") String refreshToken,
                                                              @RequestParam("clientId") String clientId);
}
