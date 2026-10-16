package com.ccloud.module.pay.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 支付单 Response VO")
@Data
@Accessors(chain = true)
public class PayOrderRespVO {

    @Schema(description = "支付单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long appId;

    @Schema(description = "渠道编号", example = "2")
    private Long channelId;

    @Schema(description = "渠道编码", example = "mock")
    private String channelCode;

    @Schema(description = "支付方式", example = "wx_lite")
    private String payMethod;

    @Schema(description = "商户订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER-20261012-0001")
    private String merchantOrderId;

    @Schema(description = "商品标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "会员月卡")
    private String subject;

    @Schema(description = "商品描述", example = "会员月卡 30 天")
    private String body;

    @Schema(description = "支付金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer price;

    @Schema(description = "渠道手续费率，单位百分比", example = "0.6")
    private Double channelFeeRate;

    @Schema(description = "渠道手续费，单位分", example = "1")
    private Integer channelFeePrice;

    @Schema(description = "支付状态，对应字典 pay_order_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "用户 IP", example = "127.0.0.1")
    private String userIp;

    @Schema(description = "失效时间")
    private LocalDateTime expireTime;

    @Schema(description = "支付成功时间")
    private LocalDateTime successTime;

    @Schema(description = "支付成功的支付尝试编号", example = "1")
    private Long extensionId;

    @Schema(description = "支付成功的外部订单号", example = "P20261012153000000001")
    private String no;

    @Schema(description = "累计退款金额（含处理中的退款），单位分", example = "0")
    private Integer refundPrice;

    @Schema(description = "渠道用户编号", example = "oX123")
    private String channelUserId;

    @Schema(description = "渠道订单号", example = "420000123")
    private String channelOrderNo;

    @Schema(description = "付款用户编号", example = "1024")
    private Long userId;

    @Schema(description = "付款用户类型，对应字典 user_type", example = "1")
    private Integer userType;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
