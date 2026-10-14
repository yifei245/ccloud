package com.ccloud.module.pay.controller.app.wallet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 钱包流水 Response VO")
@Data
@Accessors(chain = true)
public class AppPayWalletTransactionRespVO {

    @Schema(description = "流水编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "业务类型，对应字典 pay_wallet_biz_type", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Integer bizType;

    @Schema(description = "流水说明", requiredMode = Schema.RequiredMode.REQUIRED, example = "支付：订单")
    private String title;

    @Schema(description = "变动金额，单位分，正数入账负数出账", requiredMode = Schema.RequiredMode.REQUIRED, example = "-300")
    private Integer price;

    @Schema(description = "变动后余额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "700")
    private Integer balance;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
