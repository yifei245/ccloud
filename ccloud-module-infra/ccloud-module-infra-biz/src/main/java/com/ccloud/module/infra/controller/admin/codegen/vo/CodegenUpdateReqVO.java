package com.ccloud.module.infra.controller.admin.codegen.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 代码生成修改 Request VO")
@Data
public class CodegenUpdateReqVO {

    @Schema(description = "表定义", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "表定义不能为空")
    private CodegenTableSaveReqVO table;

    @Schema(description = "字段定义", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "字段定义不能为空")
    private List<CodegenColumnSaveReqVO> columns;
}
