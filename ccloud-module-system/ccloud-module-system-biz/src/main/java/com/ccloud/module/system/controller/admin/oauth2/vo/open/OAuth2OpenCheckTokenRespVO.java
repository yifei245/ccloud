package com.ccloud.module.system.controller.admin.oauth2.vo.open;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "OAuth2 开放接口 - 校验令牌 Response VO")
@Data
public class OAuth2OpenCheckTokenRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @JsonProperty("user_id")
    private Long userId;

    @Schema(description = "用户类型：1 会员 2 管理员", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @JsonProperty("user_type")
    private Integer userType;

    @Schema(description = "租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @JsonProperty("tenant_id")
    private Long tenantId;

    @Schema(description = "客户端编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ccloud-web")
    @JsonProperty("client_id")
    private String clientId;

    @Schema(description = "授权范围", example = "[\"user.read\"]")
    private List<String> scopes;

    @Schema(description = "访问令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "e1b0a9f3c6d54ef1a6b0a1c2d3e4f5a6")
    @JsonProperty("access_token")
    private String accessToken;

    @Schema(description = "过期时间，Unix 时间戳（秒）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1735660800")
    private Long exp;
}
