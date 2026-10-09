package com.ccloud.module.member.controller.admin.group.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 会员分组精简信息 Response VO")
@Data
public class MemberGroupSimpleRespVO {

    @Schema(description = "分组编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "分组名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "普通会员")
    private String name;
}
