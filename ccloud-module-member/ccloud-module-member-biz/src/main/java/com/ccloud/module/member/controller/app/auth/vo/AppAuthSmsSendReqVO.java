package com.ccloud.module.member.controller.app.auth.vo;

import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.framework.common.validation.Mobile;
import com.ccloud.module.system.enums.sms.SmsSceneEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户 App - 发送短信验证码 Request VO")
@Data
public class AppAuthSmsSendReqVO {

    @Schema(description = "手机号。修改密码场景忽略该值，固定发往当前用户的手机号", example = "13800138000")
    @Mobile
    private String mobile;

    @Schema(description = "发送场景，见 SmsSceneEnum 中的会员场景", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "发送场景不能为空")
    @InEnum(SmsSceneEnum.class)
    private Integer scene;
}
