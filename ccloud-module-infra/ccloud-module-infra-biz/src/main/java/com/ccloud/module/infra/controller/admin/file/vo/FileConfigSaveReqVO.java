package com.ccloud.module.infra.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 文件配置创建/修改 Request VO")
@Data
public class FileConfigSaveReqVO {

    @Schema(description = "编号，修改时必填", example = "1")
    private Long id;

    @Schema(description = "配置名", requiredMode = Schema.RequiredMode.REQUIRED, example = "本地磁盘")
    @NotBlank(message = "配置名不能为空")
    @Size(max = 63, message = "配置名长度不能超过 63 个字符")
    private String name;

    @Schema(description = "存储器，对应字典 infra_file_storage", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "存储器不能为空")
    private Integer storage;

    @Schema(description = "备注", example = "开发环境使用")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    @Schema(description = "存储配置，按存储器不同而不同；密钥项提交掩码表示沿用原值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "存储配置不能为空")
    private Map<String, Object> config;
}
