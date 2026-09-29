package com.ccloud.module.member.controller.app.level.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 经验记录 Response VO")
@Data
public class AppMemberExperienceRecordRespVO {

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "签到奖励")
    private String title;

    @Schema(description = "描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "签到获得 1 经验")
    private String description;

    @Schema(description = "变动经验，负数表示扣减", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer experience;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
