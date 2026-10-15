package com.ccloud.module.pay.controller.app.channel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "用户 App - 支付渠道 Response VO")
@Data
@Accessors(chain = true)
public class AppPayChannelRespVO {

    @Schema(description = "渠道编码，见字典 pay_channel_code", requiredMode = Schema.RequiredMode.REQUIRED, example = "wallet")
    private String code;

    @Schema(description = "渠道名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "钱包支付")
    private String name;
}
