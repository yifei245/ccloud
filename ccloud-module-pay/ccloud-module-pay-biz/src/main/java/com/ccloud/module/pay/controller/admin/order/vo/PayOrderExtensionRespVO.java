package com.ccloud.module.pay.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 支付尝试 Response VO")
@Data
@Accessors(chain = true)
public class PayOrderExtensionRespVO {

    @Schema(description = "支付尝试编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "外部订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "P20261012153000000001")
    private String no;

    @Schema(description = "支付单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long orderId;

    @Schema(description = "渠道编码", example = "mock")
    private String channelCode;

    @Schema(description = "支付方式", example = "wx_lite")
    private String payMethod;

    @Schema(description = "状态，对应字典 pay_order_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "渠道错误信息")
    private String channelErrorMsg;

    @Schema(description = "渠道回调原始报文，供对账排查")
    private String channelNotifyData;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
