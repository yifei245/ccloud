package com.ccloud.module.system.controller.admin.oauth2.vo.client;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - OAuth2 客户端 Response VO")
@Data
public class OAuth2ClientRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "客户端编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ccloud-web")
    private String clientId;

    @Schema(description = "客户端密钥", requiredMode = Schema.RequiredMode.REQUIRED, example = "ccloud-web-secret")
    private String secret;

    @Schema(description = "应用名", requiredMode = Schema.RequiredMode.REQUIRED, example = "管理后台")
    private String name;

    @Schema(description = "应用图标", example = "https://www.ccloud.local/logo.png")
    private String logo;

    @Schema(description = "应用描述", example = "管理后台")
    private String description;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "访问令牌的有效期，单位秒", requiredMode = Schema.RequiredMode.REQUIRED, example = "1800")
    private Integer accessTokenValiditySeconds;

    @Schema(description = "刷新令牌的有效期，单位秒", requiredMode = Schema.RequiredMode.REQUIRED, example = "2592000")
    private Integer refreshTokenValiditySeconds;

    @Schema(description = "可重定向的地址")
    private List<String> redirectUris;

    @Schema(description = "授权类型")
    private List<String> authorizedGrantTypes;

    @Schema(description = "授权范围")
    private List<String> scopes;

    @Schema(description = "自动通过的授权范围")
    private List<String> autoApproveScopes;

    @Schema(description = "权限")
    private List<String> authorities;

    @Schema(description = "资源")
    private List<String> resourceIds;

    @Schema(description = "附加信息，JSON 对象")
    private String additionalInformation;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
