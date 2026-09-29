package com.ccloud.module.member.controller.app.level.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 App - 我的等级与经验 Response VO")
@Data
public class AppMemberMyLevelRespVO {

    @Schema(description = "当前等级，没有等级时为 null")
    private AppMemberLevelRespVO level;

    @Schema(description = "当前经验", requiredMode = Schema.RequiredMode.REQUIRED, example = "1200")
    private Integer experience;
}
