package com.ccloud.module.pay.controller.app.wallet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "用户 App - 钱包充值套餐 Response VO")
@Data
@Accessors(chain = true)
public class AppPayWalletRechargePackageRespVO {

    @Schema(description = "套餐编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "套餐名", requiredMode = Schema.RequiredMode.REQUIRED, example = "充 100 送 10")
    private String name;

    @Schema(description = "支付金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000")
    private Integer payPrice;

    @Schema(description = "赠送金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
    private Integer bonusPrice;
}
