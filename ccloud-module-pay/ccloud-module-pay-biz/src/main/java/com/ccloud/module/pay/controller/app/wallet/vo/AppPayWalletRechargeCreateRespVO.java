package com.ccloud.module.pay.controller.app.wallet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "用户 App - 钱包充值创建 Response VO")
@Data
@Accessors(chain = true)
public class AppPayWalletRechargeCreateRespVO {

    @Schema(description = "充值编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "支付单编号，用它提交支付", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    private Long payOrderId;
}
