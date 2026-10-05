package com.ccloud.module.infra.controller.admin.codegen.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 代码生成预览 Response VO")
@Data
public class CodegenPreviewRespVO {

    @Schema(description = "文件路径", requiredMode = Schema.RequiredMode.REQUIRED)
    private String filePath;

    @Schema(description = "代码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}
