package com.ccloud.module.infra.controller.admin.codegen.vo;

import com.ccloud.module.infra.service.codegen.inner.CodegenValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 代码生成表定义修改 Request VO")
@Data
public class CodegenTableSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "生成场景：1 管理后台 2 用户 App", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "生成场景不能为空")
    private Integer scene;

    @Schema(description = "表描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "表描述不能为空")
    @Size(max = 500, message = "表描述长度不能超过 500 个字符")
    private String tableComment;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    @Schema(description = "模块名", requiredMode = Schema.RequiredMode.REQUIRED, example = "infra")
    @NotBlank(message = "模块名不能为空")
    @Size(max = 30, message = "模块名长度不能超过 30 个字符")
    @Pattern(regexp = CodegenValidator.PACKAGE_REGEX, message = "模块名须为小写字母开头的小写字母与数字")
    private String moduleName;

    @Schema(description = "业务名", requiredMode = Schema.RequiredMode.REQUIRED, example = "demo")
    @NotBlank(message = "业务名不能为空")
    @Size(max = 30, message = "业务名长度不能超过 30 个字符")
    @Pattern(regexp = CodegenValidator.PACKAGE_REGEX, message = "业务名须为小写字母开头的小写字母与数字")
    private String businessName;

    @Schema(description = "类名", requiredMode = Schema.RequiredMode.REQUIRED, example = "DemoStudent")
    @NotBlank(message = "类名不能为空")
    @Size(max = 100, message = "类名长度不能超过 100 个字符")
    @Pattern(regexp = CodegenValidator.CLASS_NAME_REGEX, message = "类名须为大写字母开头的字母与数字")
    private String className;

    @Schema(description = "类描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "类描述不能为空")
    @Size(max = 50, message = "类描述长度不能超过 50 个字符")
    private String classComment;

    @Schema(description = "作者", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "作者不能为空")
    @Size(max = 50, message = "作者长度不能超过 50 个字符")
    private String author;

    @Schema(description = "模板类型：1 单表 2 树表", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "模板类型不能为空")
    private Integer templateType;

    @Schema(description = "父菜单编号")
    private Long parentMenuId;

    @Schema(description = "树表的父字段编号，树表必填，须为本表中类型为 Long 的字段")
    private Long treeParentColumnId;

    @Schema(description = "树表的名称字段编号，树表必填，须为本表字段")
    private Long treeNameColumnId;
}
