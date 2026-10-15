package com.ccloud.module.pay.controller.app.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 支付单 Response VO")
@Data
@Accessors(chain = true)
public class AppPayOrderRespVO {

    @Schema(description = "支付单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long appId;

    @Schema(description = "商品标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "测试商品")
    private String subject;

    @Schema(description = "商品描述", example = "商品详情")
    private String body;

    @Schema(description = "支付金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer price;

    @Schema(description = "支付状态，见字典 pay_order_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "过期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expireTime;

    @Schema(description = "支付成功时间")
    private LocalDateTime successTime;
}
