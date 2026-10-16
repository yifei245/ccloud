package com.ccloud.module.pay.controller.admin.wallet.vo;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 钱包充值套餐创建/修改 Request VO")
@Data
@Accessors(chain = true)
public class PayWalletRechargePackageSaveReqVO {

    @Schema(description = "套餐编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "套餐名", requiredMode = Schema.RequiredMode.REQUIRED, example = "充 100 送 10")
    @NotBlank(message = "套餐名不能为空")
    @Size(max = 32, message = "套餐名长度不能超过 32 个字符")
    private String name;

    @Schema(description = "支付金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000")
    @NotNull(message = "支付金额不能为空")
    @Min(value = 1, message = "支付金额必须大于 0")
    @Max(value = 100_000_000, message = "支付金额超过上限")
    private Integer payPrice;

    @Schema(description = "赠送金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
    @NotNull(message = "赠送金额不能为空")
    @Min(value = 0, message = "赠送金额不能为负数")
    @Max(value = 100_000_000, message = "赠送金额超过上限")
    private Integer bonusPrice;

    @Schema(description = "状态，对应字典 common_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;
}
