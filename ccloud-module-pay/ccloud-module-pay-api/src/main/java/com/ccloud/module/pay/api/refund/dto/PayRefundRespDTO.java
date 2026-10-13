package com.ccloud.module.pay.api.refund.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - 退款单 Response DTO")
@Data
@Accessors(chain = true)
public class PayRefundRespDTO {

    @Schema(description = "退款单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long id;

    @Schema(description = "支付单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long orderId;

    @Schema(description = "商户订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER-20261012-0001")
    private String merchantOrderId;

    @Schema(description = "商户退款单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "REFUND-20261012-0001")
    private String merchantRefundId;

    @Schema(description = "退款金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer refundPrice;

    @Schema(description = "退款状态，见字典 pay_refund_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "退款成功时间", example = "2026-10-12 15:30:00")
    private LocalDateTime successTime;

    @Schema(description = "渠道返回的错误描述", example = "余额不足")
    private String channelErrorMsg;
}
