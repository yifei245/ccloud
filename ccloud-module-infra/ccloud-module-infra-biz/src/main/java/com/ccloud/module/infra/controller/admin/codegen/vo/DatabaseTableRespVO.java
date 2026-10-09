package com.ccloud.module.infra.controller.admin.codegen.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 数据库表 Response VO")
@Data
public class DatabaseTableRespVO {

    @Schema(description = "表名", requiredMode = Schema.RequiredMode.REQUIRED, example = "infra_demo_student")
    private String name;

    @Schema(description = "表注释", requiredMode = Schema.RequiredMode.REQUIRED)
    private String comment;
}
