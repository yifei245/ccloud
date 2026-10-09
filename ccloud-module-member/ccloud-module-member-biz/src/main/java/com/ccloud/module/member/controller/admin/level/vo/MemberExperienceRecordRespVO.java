package com.ccloud.module.member.controller.admin.level.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员经验记录 Response VO")
@Data
public class MemberExperienceRecordRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "业务类型，见字典 member_experience_biz_type", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer bizType;

    @Schema(description = "业务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private String bizId;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "下单奖励")
    private String title;

    @Schema(description = "描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "下单获得 10 经验")
    private String description;

    @Schema(description = "变动经验，负数表示扣减", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer experience;

    @Schema(description = "变动后的总经验", requiredMode = Schema.RequiredMode.REQUIRED, example = "1200")
    private Integer totalExperience;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
