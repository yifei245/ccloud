package com.ccloud.module.system.controller.admin.sso.vo;

import com.ccloud.module.system.controller.admin.oauth2.vo.open.OAuth2OpenAuthorizeInfoRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 三种结果互斥：needLogin=true 时前端先跳登录页；redirectUri 非空时直接跳转；否则展示授权页
 */
@Schema(description = "SSO - 授权检查 Response VO")
@Data
@Accessors(chain = true)
public class SsoAuthorizeRespVO {

    @Schema(description = "是否需要先登录", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean needLogin;

    @Schema(description = "已自动授权时，携带授权码的跳转地址", example = "http://localhost:8080/cb?code=0a1b2c&state=1")
    private String redirectUri;

    @Schema(description = "客户端", requiredMode = Schema.RequiredMode.REQUIRED)
    private OAuth2OpenAuthorizeInfoRespVO.Client client;

    @Schema(description = "需用户确认时，申请的授权范围及用户此前是否已同意")
    private List<OAuth2OpenAuthorizeInfoRespVO.Scope> scopes;
}
