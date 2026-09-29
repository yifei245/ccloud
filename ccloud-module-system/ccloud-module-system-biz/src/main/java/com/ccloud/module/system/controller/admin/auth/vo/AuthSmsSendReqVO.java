package com.ccloud.module.system.controller.admin.auth.vo;

import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.framework.common.validation.Mobile;
import com.ccloud.module.system.enums.sms.SmsSceneEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 发送短信验证码 Request VO")
@Data
public class AuthSmsSendReqVO {

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800000000")
    @NotBlank(message = "手机号不能为空")
    @Mobile
    private String mobile;

    @Schema(description = "发送场景：21 后台-手机号登录 22 后台-忘记密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "21")
    @NotNull(message = "发送场景不能为空")
    @InEnum(SmsSceneEnum.class)
    private Integer scene;

    @Schema(description = "图形验证码编号，开启验证码时必填", example = "a1b2c3d4e5f67890a1b2c3d4e5f67890")
    private String captchaUuid;

    @Schema(description = "图形验证码，开启验证码时必填", example = "1a2b")
    private String captchaCode;
}
