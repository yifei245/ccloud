package com.ccloud.module.infra.controller.admin.config.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ccloud.framework.excel.core.annotations.DictFormat;
import com.ccloud.framework.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 参数配置 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ConfigRespVO {

    @Schema(description = "参数编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("参数编号")
    private Long id;

    @Schema(description = "参数分组", requiredMode = Schema.RequiredMode.REQUIRED, example = "biz")
    @ExcelProperty("参数分组")
    private String category;

    @Schema(description = "参数名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "注册开关")
    @ExcelProperty("参数名称")
    private String name;

    @Schema(description = "参数键名", requiredMode = Schema.RequiredMode.REQUIRED, example = "system.auth.register-enabled")
    @ExcelProperty("参数键名")
    private String key;

    @Schema(description = "参数键值", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @ExcelProperty("参数键值")
    private String value;

    @Schema(description = "参数类型，对应字典 infra_config_type", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "参数类型", converter = DictConvert.class)
    @DictFormat("infra_config_type")
    private Integer type;

    @Schema(description = "是否可见", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @ExcelProperty("是否可见")
    private Boolean visible;

    @Schema(description = "备注", example = "用户注册开关")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
