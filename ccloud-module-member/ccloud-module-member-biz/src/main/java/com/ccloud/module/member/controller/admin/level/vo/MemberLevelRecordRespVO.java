package com.ccloud.module.member.controller.admin.level.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员等级变更记录 Response VO")
@Data
public class MemberLevelRecordRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "新等级编号，变为无等级时为 0", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Long levelId;

    @Schema(description = "新等级序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer level;

    @Schema(description = "新等级的折扣百分比", requiredMode = Schema.RequiredMode.REQUIRED, example = "98")
    private Integer discountPercent;

    @Schema(description = "新等级的升级所需经验", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
    private Integer experience;

    @Schema(description = "变更时会员的经验", requiredMode = Schema.RequiredMode.REQUIRED, example = "1200")
    private Integer userExperience;

    @Schema(description = "备注，管理员修改等级时填写的原因", example = "活动赠送")
    private String remark;

    @Schema(description = "描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "升级为白银会员")
    private String description;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
