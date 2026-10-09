package com.ccloud.module.member.controller.admin.level.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员等级 Response VO")
@Data
public class MemberLevelRespVO {

    @Schema(description = "等级编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "等级名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "白银会员")
    private String name;

    @Schema(description = "等级序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer level;

    @Schema(description = "升级所需经验", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
    private Integer experience;

    @Schema(description = "享受折扣百分比", requiredMode = Schema.RequiredMode.REQUIRED, example = "98")
    private Integer discountPercent;

    @Schema(description = "等级图标")
    private String icon;

    @Schema(description = "等级背景图")
    private String backgroundUrl;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
