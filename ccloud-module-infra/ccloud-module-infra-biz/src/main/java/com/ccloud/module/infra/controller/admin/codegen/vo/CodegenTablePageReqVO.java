package com.ccloud.module.infra.controller.admin.codegen.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 代码生成表定义分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CodegenTablePageReqVO extends PageParam {

    @Schema(description = "表名，模糊匹配", example = "infra_demo")
    private String tableName;

    @Schema(description = "表描述，模糊匹配", example = "学生")
    private String tableComment;

    @Schema(description = "类名，模糊匹配", example = "Student")
    private String className;

    @Schema(description = "创建时间区间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;
}
