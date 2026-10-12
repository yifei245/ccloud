package com.ccloud.module.open.controller.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 开放接口 Response VO")
@Data
public class OpenApiRespVO {

    @Schema(description = "接口编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "接口名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "查询会员")
    private String name;

    @Schema(description = "接口编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "member.user.get")
    private String code;

    @Schema(description = "接口路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "/open-api/member/user/get")
    private String path;

    @Schema(description = "请求方法", requiredMode = Schema.RequiredMode.REQUIRED, example = "GET")
    private String method;

    @Schema(description = "接口描述", example = "按编号查询会员")
    private String description;

    @Schema(description = "状态，对应字典 common_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
