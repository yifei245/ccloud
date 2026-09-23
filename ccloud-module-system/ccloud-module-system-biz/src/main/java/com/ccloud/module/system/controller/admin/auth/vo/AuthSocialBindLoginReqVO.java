package com.ccloud.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 不支持按租户名登录：社交账号在请求头的租户下查找与绑定，账密也必须在同一租户下校验
 */
@Schema(description = "管理后台 - 社交登录并绑定已有账号 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AuthSocialBindLoginReqVO extends AuthSocialLoginReqVO {

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    @NotBlank(message = "登录账号不能为空")
    @Size(min = 4, max = 30, message = "账号长度为 4-30 位")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin123")
    @NotBlank(message = "密码不能为空")
    @Size(min = 4, max = 16, message = "密码长度为 4-16 位")
    private String password;

    @Schema(description = "图形验证码编号，开启验证码时必填", example = "a1b2c3d4e5f67890a1b2c3d4e5f67890")
    private String captchaUuid;

    @Schema(description = "图形验证码，开启验证码时必填", example = "1a2b")
    private String captchaCode;
}
