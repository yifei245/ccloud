package com.ccloud.module.infra.controller.admin.codegen.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 代码生成字段定义 Response VO")
@Data
public class CodegenColumnRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "表编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long tableId;

    @Schema(description = "字段名", requiredMode = Schema.RequiredMode.REQUIRED, example = "user_age")
    private String columnName;

    @Schema(description = "字段类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "varchar(100)")
    private String dataType;

    @Schema(description = "字段描述", requiredMode = Schema.RequiredMode.REQUIRED)
    private String columnComment;

    @Schema(description = "是否允许为空", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean nullable;

    @Schema(description = "是否主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean primaryKey;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer ordinalPosition;

    @Schema(description = "Java 属性类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "Integer")
    private String javaType;

    @Schema(description = "Java 属性名", requiredMode = Schema.RequiredMode.REQUIRED, example = "userAge")
    private String javaField;

    @Schema(description = "字典类型", example = "system_user_sex")
    private String dictType;

    @Schema(description = "数据示例", example = "1024")
    private String example;

    @Schema(description = "是否为新增操作的字段", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean createOperation;

    @Schema(description = "是否为更新操作的字段", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean updateOperation;

    @Schema(description = "是否为查询条件字段", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean listOperation;

    @Schema(description = "查询条件类型：=、!=、>、>=、<、<=、LIKE、BETWEEN", requiredMode = Schema.RequiredMode.REQUIRED, example = "=")
    private String listOperationCondition;

    @Schema(description = "是否为列表结果字段", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean listOperationResult;

    @Schema(description = "显示类型：input、textarea、select、radio、datetime", requiredMode = Schema.RequiredMode.REQUIRED, example = "input")
    private String htmlType;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;
}
