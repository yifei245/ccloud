package com.ccloud.module.infra.controller.admin.codegen.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 代码生成字段定义修改 Request VO")
@Data
public class CodegenColumnSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "字段描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字段描述不能为空")
    @Size(max = 500, message = "字段描述长度不能超过 500 个字符")
    private String columnComment;

    @Schema(description = "Java 属性类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "Integer")
    @NotBlank(message = "Java 属性类型不能为空")
    @Size(max = 32, message = "Java 属性类型长度不能超过 32 个字符")
    private String javaType;

    @Schema(description = "Java 属性名", requiredMode = Schema.RequiredMode.REQUIRED, example = "userAge")
    @NotBlank(message = "Java 属性名不能为空")
    @Size(max = 64, message = "Java 属性名长度不能超过 64 个字符")
    private String javaField;

    @Schema(description = "字典类型", example = "system_user_sex")
    @Size(max = 200, message = "字典类型长度不能超过 200 个字符")
    private String dictType;

    @Schema(description = "数据示例", example = "1024")
    @Size(max = 64, message = "数据示例长度不能超过 64 个字符")
    private String example;

    @Schema(description = "是否为新增操作的字段", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否为新增操作的字段不能为空")
    private Boolean createOperation;

    @Schema(description = "是否为更新操作的字段", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否为更新操作的字段不能为空")
    private Boolean updateOperation;

    @Schema(description = "是否为查询条件字段", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否为查询条件字段不能为空")
    private Boolean listOperation;

    @Schema(description = "查询条件类型：=、!=、>、>=、<、<=、LIKE、BETWEEN", requiredMode = Schema.RequiredMode.REQUIRED, example = "=")
    @NotBlank(message = "查询条件类型不能为空")
    @Size(max = 32, message = "查询条件类型长度不能超过 32 个字符")
    private String listOperationCondition;

    @Schema(description = "是否为列表结果字段", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否为列表结果字段不能为空")
    private Boolean listOperationResult;

    @Schema(description = "显示类型：input、textarea、select、radio、datetime", requiredMode = Schema.RequiredMode.REQUIRED, example = "input")
    @NotBlank(message = "显示类型不能为空")
    @Size(max = 32, message = "显示类型长度不能超过 32 个字符")
    private String htmlType;
}
