package com.ccloud.module.system.controller.admin.oauth2.vo.client;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.module.system.enums.oauth2.OAuth2GrantTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Schema(description = "管理后台 - OAuth2 客户端创建/修改 Request VO")
@Data
public class OAuth2ClientSaveReqVO {

    @Schema(description = "编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "客户端编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ccloud-web")
    @NotBlank(message = "客户端编号不能为空")
    @Size(max = 255, message = "客户端编号长度不能超过 255 个字符")
    private String clientId;

    @Schema(description = "客户端密钥", requiredMode = Schema.RequiredMode.REQUIRED, example = "ccloud-web-secret")
    @NotBlank(message = "客户端密钥不能为空")
    @Size(min = 16, max = 255, message = "客户端密钥长度为 16-255 个字符")
    private String secret;

    @Schema(description = "应用名", requiredMode = Schema.RequiredMode.REQUIRED, example = "管理后台")
    @NotBlank(message = "应用名不能为空")
    private String name;

    @Schema(description = "应用图标", example = "https://www.ccloud.local/logo.png")
    private String logo = "";

    @Schema(description = "应用描述", example = "管理后台")
    private String description;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "访问令牌的有效期，单位秒", requiredMode = Schema.RequiredMode.REQUIRED, example = "1800")
    @NotNull(message = "访问令牌的有效期不能为空")
    @Min(value = 60, message = "访问令牌的有效期不能少于 60 秒")
    private Integer accessTokenValiditySeconds;

    @Schema(description = "刷新令牌的有效期，单位秒，0 表示不签发刷新令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "2592000")
    @NotNull(message = "刷新令牌的有效期不能为空")
    @Min(value = 0, message = "刷新令牌的有效期不能为负数")
    private Integer refreshTokenValiditySeconds;

    @Schema(description = "可重定向的地址，必须与授权请求中的地址完全一致", example = "[\"http://localhost:8080/cb\"]")
    private List<@NotBlank(message = "重定向地址不能为空") String> redirectUris = Collections.emptyList();

    @Schema(description = "授权类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"password\"]")
    @NotEmpty(message = "授权类型不能为空")
    private List<String> authorizedGrantTypes;

    @Schema(description = "授权范围", example = "[\"user.read\"]")
    private List<String> scopes = Collections.emptyList();

    @Schema(description = "自动通过的授权范围，须为授权范围的子集", example = "[\"user.read\"]")
    private List<String> autoApproveScopes = Collections.emptyList();

    @Schema(description = "权限", example = "[]")
    private List<String> authorities = Collections.emptyList();

    @Schema(description = "资源", example = "[]")
    private List<String> resourceIds = Collections.emptyList();

    @Schema(description = "附加信息，JSON 对象", example = "{}")
    private String additionalInformation = "{}";

    @AssertTrue(message = "授权类型不合法")
    @JsonIgnore
    public boolean isAuthorizedGrantTypesValid() {
        List<String> types = Arrays.stream(OAuth2GrantTypeEnum.values()).map(OAuth2GrantTypeEnum::getGrantType).toList();
        return authorizedGrantTypes == null || types.containsAll(authorizedGrantTypes);
    }

    @AssertTrue(message = "自动通过的授权范围必须包含在授权范围内")
    @JsonIgnore
    public boolean isAutoApproveScopesValid() {
        return CollUtil.isEmpty(autoApproveScopes) || (scopes != null && scopes.containsAll(autoApproveScopes));
    }

    @AssertTrue(message = "授权码模式必须配置重定向地址")
    @JsonIgnore
    public boolean isRedirectUrisValid() {
        return authorizedGrantTypes == null
                || !authorizedGrantTypes.contains(OAuth2GrantTypeEnum.AUTHORIZATION_CODE.getGrantType())
                || CollUtil.isNotEmpty(redirectUris);
    }
}
