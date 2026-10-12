package com.ccloud.module.open.controller.admin.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Schema(description = "管理后台 - 开放应用授权接口 Request VO")
@Data
public class OpenAppAssignApiReqVO {

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "应用编号不能为空")
    private Long appId;

    @Schema(description = "接口编号集合，整体替换原有授权；空集合表示取消全部授权", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "接口编号集合不能为空")
    private Set<Long> apiIds;
}
