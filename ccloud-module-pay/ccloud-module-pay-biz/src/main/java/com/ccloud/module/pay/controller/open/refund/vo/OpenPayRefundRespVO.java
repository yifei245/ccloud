package com.ccloud.module.pay.controller.open.refund.vo;

import com.ccloud.module.pay.controller.open.json.OpenEpochMilliSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "开放接口 - 退款单 Response VO，时间字段均为毫秒时间戳")
@Data
@Accessors(chain = true)
public class OpenPayRefundRespVO {

    @Schema(description = "商户订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER-20261012-0001")
    private String merchantOrderId;

    @Schema(description = "商户退款单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "REFUND-20261012-0001")
    private String merchantRefundId;

    @Schema(description = "外部退款单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "R20261012153000000001")
    private String refundNo;

    @Schema(description = "退款金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    private Integer refundPrice;

    @Schema(description = "退款状态：0 未退款（处理中），10 退款成功，20 退款失败", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "退款成功时间，毫秒时间戳（系统时区）；未成功为空", type = "integer", format = "int64", example = "1791000000000")
    @JsonSerialize(using = OpenEpochMilliSerializer.class)
    private LocalDateTime successTime;

    @Schema(description = "渠道返回的失败原因", example = "余额不足")
    private String channelErrorMsg;
}
