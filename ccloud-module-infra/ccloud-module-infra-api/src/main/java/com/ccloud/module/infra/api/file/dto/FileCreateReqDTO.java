package com.ccloud.module.infra.api.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "RPC 服务 - 保存文件 Request DTO")
@Data
public class FileCreateReqDTO {

    @Schema(description = "原始文件名，用于辅助识别类型，可为空", example = "avatar.png")
    private String name;

    @Schema(description = "文件内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "文件内容不能为空")
    private byte[] content;
}
