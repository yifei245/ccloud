package com.ccloud.module.infra.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 文件 Response VO")
@Data
public class FileRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "文件配置编号", example = "1")
    private Long configId;

    @Schema(description = "原始文件名", example = "avatar.png")
    private String name;

    @Schema(description = "存储路径", requiredMode = Schema.RequiredMode.REQUIRED)
    private String path;

    @Schema(description = "访问地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;

    @Schema(description = "MIME 类型", example = "image/png")
    private String type;

    @Schema(description = "文件大小，单位字节", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Integer size;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
