package com.ccloud.module.pay.controller.app.wallet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "用户 App - 钱包充值创建 Request VO")
@Data
@Accessors(chain = true)
public class AppPayWalletRechargeCreateReqVO {

    @Schema(description = "充值套餐编号；选了套餐则按套餐金额充值并赠送", example = "1")
    private Long packageId;

    @Schema(description = "自定义充值金额，单位分；未选套餐时必填，无赠送", example = "5000")
    @Min(value = 1, message = "充值金额必须大于 0")
    @Max(value = 100_000_000, message = "充值金额超过上限")
    private Integer payPrice;
}
