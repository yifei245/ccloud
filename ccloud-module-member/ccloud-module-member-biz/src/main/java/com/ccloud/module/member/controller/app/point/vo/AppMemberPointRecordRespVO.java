package com.ccloud.module.member.controller.app.point.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 积分记录 Response VO")
@Data
public class AppMemberPointRecordRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "签到")
    private String title;

    @Schema(description = "描述", example = "签到获得 2 积分")
    private String description;

    @Schema(description = "变动积分，负数表示扣减", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer point;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
