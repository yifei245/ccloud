package com.ccloud.module.system.controller.admin.sso.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Schema(description = "SSO - 统一登出 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SsoLogoutRespVO {

    @Schema(description = "登出后跳转地址，已校验为客户端登记的地址", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "http://localhost:8080/cb")
    private String redirectUri;

    @Schema(description = "用户授权过的客户端，前端据此清理各应用的本地登录态", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "[\"ccloud-web\"]")
    private Set<String> clientIds;
}
