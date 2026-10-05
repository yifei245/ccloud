package com.ccloud.module.infra.controller.admin.codegen.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 代码生成表定义 Response VO")
@Data
public class CodegenTableRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "数据源配置编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Long dataSourceConfigId;

    @Schema(description = "生成场景：1 管理后台 2 用户 App", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer scene;

    @Schema(description = "表名", requiredMode = Schema.RequiredMode.REQUIRED, example = "infra_demo_student")
    private String tableName;

    @Schema(description = "表描述", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tableComment;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "模块名", requiredMode = Schema.RequiredMode.REQUIRED, example = "infra")
    private String moduleName;

    @Schema(description = "业务名", requiredMode = Schema.RequiredMode.REQUIRED, example = "demo")
    private String businessName;

    @Schema(description = "类名", requiredMode = Schema.RequiredMode.REQUIRED, example = "DemoStudent")
    private String className;

    @Schema(description = "类描述", requiredMode = Schema.RequiredMode.REQUIRED)
    private String classComment;

    @Schema(description = "作者", requiredMode = Schema.RequiredMode.REQUIRED)
    private String author;

    @Schema(description = "模板类型：1 单表 2 树表", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer templateType;

    @Schema(description = "前端类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer frontType;

    @Schema(description = "父菜单编号")
    private Long parentMenuId;

    @Schema(description = "主表编号，子表生成时使用（预留）")
    private Long masterTableId;

    @Schema(description = "子表关联主表的字段编号（预留）")
    private Long subJoinColumnId;

    @Schema(description = "主表与子表是否一对多（预留）")
    private Boolean subJoinMany;

    @Schema(description = "树表的父字段编号")
    private Long treeParentColumnId;

    @Schema(description = "树表的名称字段编号")
    private Long treeNameColumnId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;
}
