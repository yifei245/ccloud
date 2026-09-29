package com.ccloud.module.system.controller.admin.captcha.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 图形验证码 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaRespVO {

    @Schema(description = "验证码编号，登录时回传", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "a1b2c3d4e5f67890a1b2c3d4e5f67890")
    private String uuid;

    @Schema(description = "验证码图片，base64 的 data URI", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "data:image/png;base64,iVBORw0KGgo=")
    private String img;
}
