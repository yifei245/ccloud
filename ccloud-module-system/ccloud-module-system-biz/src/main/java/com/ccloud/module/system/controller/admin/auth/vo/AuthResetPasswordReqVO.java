package com.ccloud.module.system.controller.admin.auth.vo;

import com.ccloud.framework.common.validation.Mobile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 短信验证码重置密码 Request VO")
@Data
public class AuthResetPasswordReqVO {

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800000000")
    @NotBlank(message = "手机号不能为空")
    @Mobile
    private String mobile;

    @Schema(description = "短信验证码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "验证码不能为空")
    private String code;

    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "new123")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 4, max = 16, message = "密码长度为 4-16 位")
    private String password;
}
