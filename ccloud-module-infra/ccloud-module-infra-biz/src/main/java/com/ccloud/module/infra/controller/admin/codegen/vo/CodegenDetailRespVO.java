package com.ccloud.module.infra.controller.admin.codegen.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 代码生成详情 Response VO")
@Data
public class CodegenDetailRespVO {

    @Schema(description = "表定义", requiredMode = Schema.RequiredMode.REQUIRED)
    private CodegenTableRespVO table;

    @Schema(description = "字段定义", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<CodegenColumnRespVO> columns;
}
