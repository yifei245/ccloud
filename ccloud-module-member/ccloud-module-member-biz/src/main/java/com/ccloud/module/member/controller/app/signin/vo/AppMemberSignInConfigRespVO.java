package com.ccloud.module.member.controller.app.signin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 App - 签到规则 Response VO")
@Data
public class AppMemberSignInConfigRespVO {

    @Schema(description = "连续签到第几天", requiredMode = Schema.RequiredMode.REQUIRED, example = "7")
    private Integer day;

    @Schema(description = "奖励积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "14")
    private Integer point;

    @Schema(description = "奖励经验", requiredMode = Schema.RequiredMode.REQUIRED, example = "7")
    private Integer experience;
}
