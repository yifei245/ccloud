package com.ccloud.module.pay.controller.app.wallet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 钱包充值记录 Response VO")
@Data
@Accessors(chain = true)
public class AppPayWalletRechargeRespVO {

    @Schema(description = "充值编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "到账总金额（实付加赠送），单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "11000")
    private Integer totalPrice;

    @Schema(description = "实际支付金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000")
    private Integer payPrice;

    @Schema(description = "赠送金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
    private Integer bonusPrice;

    @Schema(description = "是否已支付", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean payStatus;

    @Schema(description = "支付单编号", example = "4096")
    private Long payOrderId;

    @Schema(description = "支付成功时间")
    private LocalDateTime payTime;

    @Schema(description = "退款状态，对应字典 pay_refund_status", example = "0")
    private Integer refundStatus;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
