package com.ccloud.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 注册 Request VO")
@Data
public class AuthRegisterReqVO {

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "newbie")
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,30}$", message = "账号由 4-30 位数字或字母组成")
    private String username;

    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "新用户")
    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称长度不能超过 30 个字符")
    private String nickname;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "new123")
    @NotBlank(message = "密码不能为空")
    @Size(min = 4, max = 16, message = "密码长度为 4-16 位")
    private String password;

    @Schema(description = "验证码编号，开启验证码时必填", example = "a1b2c3d4e5f67890a1b2c3d4e5f67890")
    private String captchaUuid;

    @Schema(description = "验证码，开启验证码时必填", example = "1a2b")
    private String captchaCode;
}
