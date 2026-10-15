package com.ccloud.module.pay.controller.open.order.vo;

import com.ccloud.module.pay.controller.open.json.OpenEpochMilliSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "开放接口 - 支付单 Response VO，时间字段均为毫秒时间戳")
@Data
@Accessors(chain = true)
public class OpenPayOrderRespVO {

    @Schema(description = "商户订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER-20261012-0001")
    private String merchantOrderId;

    @Schema(description = "支付成功的外部订单号，未支付成功为空", example = "P20261012153000000001")
    private String no;

    @Schema(description = "商品标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "测试商品")
    private String subject;

    @Schema(description = "支付金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer price;

    @Schema(description = "支付状态：0 待支付，10 支付成功，20 已退款，30 已关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "已退款金额，单位分", example = "0")
    private Integer refundPrice;

    @Schema(description = "支付渠道编码", example = "mock")
    private String channelCode;

    @Schema(description = "支付方式", example = "wx_lite")
    private String payMethod;

    @Schema(description = "过期时间，毫秒时间戳，精度为秒", type = "integer", format = "int64", example = "1791000000000")
    @JsonSerialize(using = OpenEpochMilliSerializer.class)
    private LocalDateTime expireTime;

    @Schema(description = "支付成功时间，毫秒时间戳，精度为秒；未支付成功为空", type = "integer", format = "int64", example = "1791000000000")
    @JsonSerialize(using = OpenEpochMilliSerializer.class)
    private LocalDateTime successTime;
}
