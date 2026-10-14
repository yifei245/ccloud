package com.ccloud.module.pay.controller.admin.wallet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 会员钱包调整余额 Request VO")
@Data
public class PayWalletUpdateBalanceReqVO {

    @Schema(description = "钱包编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "钱包编号不能为空")
    private Long id;

    @Schema(description = "调整金额，单位分，正数增加负数减少，不能为 0", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "调整金额不能为空")
    private Integer price;

    @Schema(description = "调整原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "客诉补偿")
    @NotBlank(message = "调整原因不能为空")
    @Size(max = 100, message = "调整原因不能超过 100 个字符")
    private String reason;
}
