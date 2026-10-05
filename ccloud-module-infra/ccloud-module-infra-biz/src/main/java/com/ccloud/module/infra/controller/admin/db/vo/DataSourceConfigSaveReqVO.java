package com.ccloud.module.infra.controller.admin.db.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Schema(description = "管理后台 - 数据源配置创建/修改 Request VO")
@Data
@ToString(exclude = "password")
public class DataSourceConfigSaveReqVO {

    @Schema(description = "主键编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "数据源名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "测试数据源")
    @NotBlank(message = "数据源名称不能为空")
    @Size(max = 100, message = "数据源名称长度不能超过 100 个字符")
    private String name;

    @Schema(description = "数据源连接，只支持 MySQL", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "jdbc:mysql://127.0.0.1:3306/ccloud")
    @NotBlank(message = "数据源连接不能为空")
    @Size(max = 1024, message = "数据源连接长度不能超过 1024 个字符")
    private String url;

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "root")
    @NotBlank(message = "用户名不能为空")
    @Size(max = 255, message = "用户名长度不能超过 255 个字符")
    private String username;

    @Schema(description = "密码，新增时必填；修改时提交掩码或留空表示沿用原密码", example = "123456")
    private String password;
}
