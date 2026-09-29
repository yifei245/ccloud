package com.ccloud.module.system.controller.admin.oauth2.vo.open;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段名遵循 OAuth2 规范（RFC 6749 5.1），便于标准客户端库直接解析
 */
@Schema(description = "OAuth2 开放接口 - 访问令牌 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2OpenAccessTokenRespVO {

    @Schema(description = "访问令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "e1b0a9f3c6d54ef1a6b0a1c2d3e4f5a6")
    @JsonProperty("access_token")
    private String accessToken;

    @Schema(description = "刷新令牌，未签发时为空", example = "0a1b2c3d4e5f67890a1b2c3d4e5f6789")
    @JsonProperty("refresh_token")
    private String refreshToken;

    @Schema(description = "令牌类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "bearer")
    @JsonProperty("token_type")
    private String tokenType;

    @Schema(description = "剩余有效期，单位秒", requiredMode = Schema.RequiredMode.REQUIRED, example = "1799")
    @JsonProperty("expires_in")
    private Long expiresIn;

    @Schema(description = "授权范围，空格分隔", example = "user.read user.write")
    private String scope;
}
