package com.ccloud.module.pay.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "支付单提交支付 Response VO，会员端与开放接口复用")
@Data
@Accessors(chain = true)
public class PayOrderSubmitRespVO {

    @Schema(description = "支付单状态，见字典 pay_order_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "展示模式：QR_CODE、URL、FORM、APP、NONE", example = "URL")
    private String displayMode;

    @Schema(description = "展示内容，按展示模式解释", example = "https://pay.example.com/pay?no=P20261012153000000001")
    private String displayContent;

    @Schema(description = "本次支付尝试的外部订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "P20261012153000000001")
    private String no;
}
