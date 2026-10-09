package com.ccloud.module.infra.controller.admin.db.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 数据源配置 Response VO")
@Data
public class DataSourceConfigRespVO {

    @Schema(description = "主键编号，0 为主数据源", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "数据源名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "测试数据源")
    private String name;

    @Schema(description = "数据源连接", requiredMode = Schema.RequiredMode.REQUIRED, example = "jdbc:mysql://127.0.0.1:3306/ccloud")
    private String url;

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "root")
    private String username;

    @Schema(description = "创建时间，主数据源为空")
    private LocalDateTime createTime;
}
