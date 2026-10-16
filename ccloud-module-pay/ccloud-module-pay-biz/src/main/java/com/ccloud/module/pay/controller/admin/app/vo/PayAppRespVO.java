package com.ccloud.module.pay.controller.admin.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 支付应用 Response VO")
@Data
@Accessors(chain = true)
public class PayAppRespVO {

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "应用标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "mall-shop")
    private String appKey;

    @Schema(description = "应用名", requiredMode = Schema.RequiredMode.REQUIRED, example = "商城")
    private String name;

    @Schema(description = "状态，对应字典 common_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "绑定的开放应用编号，为空表示未绑定", example = "2048")
    private Long openAppId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
