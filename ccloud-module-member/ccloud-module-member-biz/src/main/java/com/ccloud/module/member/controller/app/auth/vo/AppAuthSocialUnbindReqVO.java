package com.ccloud.module.member.controller.app.auth.vo;

import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.module.system.enums.social.SocialTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户 App - 解绑社交账号 Request VO")
@Data
public class AppAuthSocialUnbindReqVO {

    @Schema(description = "社交平台，见 SocialTypeEnum", requiredMode = Schema.RequiredMode.REQUIRED, example = "31")
    @NotNull(message = "社交平台不能为空")
    @InEnum(SocialTypeEnum.class)
    private Integer type;

    @Schema(description = "社交平台 openid", requiredMode = Schema.RequiredMode.REQUIRED, example = "oXyz123")
    @NotBlank(message = "openid 不能为空")
    private String openid;
}
