package com.ccloud.module.member.controller.admin.level.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 会员等级精简信息 Response VO")
@Data
public class MemberLevelSimpleRespVO {

    @Schema(description = "等级编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "等级名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "白银会员")
    private String name;

    @Schema(description = "等级序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer level;
}
