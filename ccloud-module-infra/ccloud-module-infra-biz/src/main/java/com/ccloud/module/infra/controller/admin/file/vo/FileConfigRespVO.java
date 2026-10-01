package com.ccloud.module.infra.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 文件配置 Response VO")
@Data
public class FileConfigRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "配置名", requiredMode = Schema.RequiredMode.REQUIRED, example = "数据库存储")
    private String name;

    @Schema(description = "存储器，对应字典 infra_file_storage", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer storage;

    @Schema(description = "是否为主配置", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean master;

    @Schema(description = "存储配置，密钥项显示为掩码", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> config;

    @Schema(description = "备注", example = "默认主配置")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
