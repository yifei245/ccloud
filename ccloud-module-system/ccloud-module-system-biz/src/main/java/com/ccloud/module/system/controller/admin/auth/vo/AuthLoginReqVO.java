package com.ccloud.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 账号密码登录 Request VO")
@Data
public class AuthLoginReqVO {

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    @NotBlank(message = "登录账号不能为空")
    @Size(min = 4, max = 30, message = "账号长度为 4-30 位")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin123")
    @NotBlank(message = "密码不能为空")
    @Size(min = 4, max = 16, message = "密码长度为 4-16 位")
    private String password;

    @Schema(description = "验证码编号，开启验证码时必填", example = "a1b2c3d4e5f67890a1b2c3d4e5f67890")
    private String captchaUuid;

    @Schema(description = "验证码，开启验证码时必填", example = "1a2b")
    private String captchaCode;

    @Schema(description = "租户名，未传时使用请求头中的租户编号", example = "ccloud 总部")
    private String tenantName;
}
