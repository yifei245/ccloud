package com.ccloud.module.pay.controller.admin.refund.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 退款单 Response VO")
@Data
@Accessors(chain = true)
public class PayRefundRespVO {

    @Schema(description = "退款单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long id;

    @Schema(description = "外部退款单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "R20261012153000000001")
    private String no;

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long appId;

    @Schema(description = "渠道编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Long channelId;

    @Schema(description = "渠道编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "mock")
    private String channelCode;

    @Schema(description = "支付单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long orderId;

    @Schema(description = "支付单的外部订单号", example = "P20261012153000000001")
    private String orderNo;

    @Schema(description = "商户订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER-20261012-0001")
    private String merchantOrderId;

    @Schema(description = "商户退款单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "REFUND-20261012-0001")
    private String merchantRefundId;

    @Schema(description = "退款状态，对应字典 pay_refund_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "原支付金额，单位分", example = "100")
    private Integer payPrice;

    @Schema(description = "退款金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer refundPrice;

    @Schema(description = "退款原因", example = "商品缺货")
    private String reason;

    @Schema(description = "用户 IP", example = "127.0.0.1")
    private String userIp;

    @Schema(description = "渠道订单号", example = "420000123")
    private String channelOrderNo;

    @Schema(description = "渠道退款单号", example = "50000123")
    private String channelRefundNo;

    @Schema(description = "退款成功时间")
    private LocalDateTime successTime;

    @Schema(description = "渠道错误码", example = "MOCK_FAIL")
    private String channelErrorCode;

    @Schema(description = "渠道错误信息", example = "余额不足")
    private String channelErrorMsg;

    @Schema(description = "渠道回调原始报文，供排查")
    private String channelNotifyData;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
