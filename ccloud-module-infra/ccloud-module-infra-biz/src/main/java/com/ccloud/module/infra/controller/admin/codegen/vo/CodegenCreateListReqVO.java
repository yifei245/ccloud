package com.ccloud.module.infra.controller.admin.codegen.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 代码生成导入表 Request VO")
@Data
public class CodegenCreateListReqVO {

    @Schema(description = "数据源配置编号，0 为主数据源", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "数据源配置编号不能为空")
    private Long dataSourceConfigId;

    @Schema(description = "要导入的表名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "表名不能为空")
    private List<String> tableNames;
}
