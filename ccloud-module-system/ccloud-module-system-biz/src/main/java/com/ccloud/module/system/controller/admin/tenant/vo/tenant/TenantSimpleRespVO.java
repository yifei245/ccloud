package com.ccloud.module.system.controller.admin.tenant.vo.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录前匿名可访问的接口只返回编号与名称，避免泄露联系人等信息
 */
@Schema(description = "管理后台 - 租户精简信息 Response VO")
@Data
public class TenantSimpleRespVO {

    @Schema(description = "租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "租户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "ccloud")
    private String name;
}
