package com.ccloud.module.pay.controller.admin.channel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 支付渠道 Response VO")
@Data
@ToString(exclude = "config")
@Accessors(chain = true)
public class PayChannelRespVO {

    @Schema(description = "渠道编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long appId;

    @Schema(description = "渠道编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "mock")
    private String code;

    @Schema(description = "状态，对应字典 common_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "渠道费率，单位百分比", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.6")
    private Double feeRate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "渠道配置 JSON，敏感字段已脱敏为 ***", requiredMode = Schema.RequiredMode.REQUIRED)
    private String config;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
