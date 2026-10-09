package com.ccloud.module.member.controller.admin.signin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 签到记录 Response VO")
@Data
public class MemberSignInRecordRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "连续签到第几天", requiredMode = Schema.RequiredMode.REQUIRED, example = "7")
    private Integer day;

    @Schema(description = "获得的积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "14")
    private Integer point;

    @Schema(description = "获得的经验", requiredMode = Schema.RequiredMode.REQUIRED, example = "7")
    private Integer experience;

    @Schema(description = "签到时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
