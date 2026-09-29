package com.ccloud.module.member.controller.app.auth.vo;

import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.module.system.enums.social.SocialTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户 App - 绑定社交账号 Request VO")
@Data
public class AppAuthSocialBindReqVO {

    @Schema(description = "社交平台，见 SocialTypeEnum", requiredMode = Schema.RequiredMode.REQUIRED, example = "31")
    @NotNull(message = "社交平台不能为空")
    @InEnum(SocialTypeEnum.class)
    private Integer type;

    @Schema(description = "授权码", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotBlank(message = "授权码不能为空")
    private String code;

    @Schema(description = "state", requiredMode = Schema.RequiredMode.REQUIRED, example = "9b2ffbc1-7425-4155-9894-9d5c08541d62")
    @NotBlank(message = "state 不能为空")
    private String state;
}
