package com.ccloud.module.member.controller.app.level.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 App - 会员等级 Response VO")
@Data
public class AppMemberLevelRespVO {

    @Schema(description = "等级编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Long id;

    @Schema(description = "等级名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "白银会员")
    private String name;

    @Schema(description = "等级序号，数值越大等级越高", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer level;

    @Schema(description = "升级所需经验", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
    private Integer experience;

    @Schema(description = "享受折扣百分比，100 表示不打折", requiredMode = Schema.RequiredMode.REQUIRED, example = "98")
    private Integer discountPercent;

    @Schema(description = "等级图标")
    private String icon;

    @Schema(description = "等级背景图")
    private String backgroundUrl;
}
