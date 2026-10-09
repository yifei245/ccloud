package com.ccloud.module.member.controller.app.signin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 App - 签到概况 Response VO")
@Data
public class AppMemberSignInRecordSummaryRespVO {

    @Schema(description = "今天是否已签到", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean todaySignIn;

    @Schema(description = "当前连续签到天数，已断签时为 0", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Integer continuousDay;

    @Schema(description = "累计签到天数", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer totalDay;
}
