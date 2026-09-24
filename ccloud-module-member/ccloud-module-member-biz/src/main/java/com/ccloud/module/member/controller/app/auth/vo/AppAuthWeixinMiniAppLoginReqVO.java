package com.ccloud.module.member.controller.app.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "用户 App - 微信小程序一键登录 Request VO")
@Data
public class AppAuthWeixinMiniAppLoginReqVO {

    @Schema(description = "wx.login 返回的登录凭证", requiredMode = Schema.RequiredMode.REQUIRED, example = "0e1a2b3c")
    @NotBlank(message = "登录凭证不能为空")
    private String loginCode;

    @Schema(description = "getPhoneNumber 返回的手机号凭证", requiredMode = Schema.RequiredMode.REQUIRED, example = "9f8e7d6c")
    @NotBlank(message = "手机号凭证不能为空")
    private String phoneCode;
}
