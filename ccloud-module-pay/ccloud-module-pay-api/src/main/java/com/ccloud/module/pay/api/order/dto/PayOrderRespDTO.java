package com.ccloud.module.pay.api.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - 支付单 Response DTO")
@Data
@Accessors(chain = true)
public class PayOrderRespDTO {

    @Schema(description = "支付单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "支付应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long appId;

    @Schema(description = "商户订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER-20261012-0001")
    private String merchantOrderId;

    @Schema(description = "商品标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "会员月卡")
    private String subject;

    @Schema(description = "支付金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer price;

    @Schema(description = "支付状态，见字典 pay_order_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "支付渠道编码，见字典 pay_channel_code", example = "mock")
    private String channelCode;

    @Schema(description = "支付方式", example = "qr")
    private String payMethod;

    @Schema(description = "支付成功时间", example = "2026-10-12 15:30:00")
    private LocalDateTime successTime;

    @Schema(description = "已退款金额，单位分", example = "0")
    private Integer refundPrice;

    @Schema(description = "外部订单号", example = "P20261012153000000001")
    private String no;

    @Schema(description = "支付过期时间", example = "2026-10-12 18:00:00")
    private LocalDateTime expireTime;
}
