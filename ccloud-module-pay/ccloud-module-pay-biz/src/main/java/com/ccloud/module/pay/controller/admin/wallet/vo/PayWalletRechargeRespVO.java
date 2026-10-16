package com.ccloud.module.pay.controller.admin.wallet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 钱包充值记录 Response VO")
@Data
@Accessors(chain = true)
public class PayWalletRechargeRespVO {

    @Schema(description = "充值编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "钱包编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long walletId;

    @Schema(description = "到账总金额（实付加赠送），单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "11000")
    private Integer totalPrice;

    @Schema(description = "实际支付金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000")
    private Integer payPrice;

    @Schema(description = "赠送金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
    private Integer bonusPrice;

    @Schema(description = "充值套餐编号，自定义金额时为空", example = "1")
    private Long packageId;

    @Schema(description = "是否已支付", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean payStatus;

    @Schema(description = "支付单编号", example = "4096")
    private Long payOrderId;

    @Schema(description = "支付渠道编码，对应字典 pay_channel_code", example = "wx_lite")
    private String payChannelCode;

    @Schema(description = "支付成功时间")
    private LocalDateTime payTime;

    @Schema(description = "退款单编号，非空即已发起退款", example = "8192")
    private Long payRefundId;

    @Schema(description = "退款扣回的到账总金额，单位分", example = "11000")
    private Integer refundTotalPrice;

    @Schema(description = "退款的实付金额，单位分", example = "10000")
    private Integer refundPayPrice;

    @Schema(description = "退款扣回的赠送金额，单位分", example = "1000")
    private Integer refundBonusPrice;

    @Schema(description = "退款成功时间")
    private LocalDateTime refundTime;

    @Schema(description = "退款状态，对应字典 pay_refund_status；未退款与退款中均为 0，以退款单编号区分", example = "0")
    private Integer refundStatus;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
