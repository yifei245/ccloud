package com.ccloud.module.pay.controller.admin.wallet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员钱包 Response VO")
@Data
@Accessors(chain = true)
public class PayWalletRespVO {

    @Schema(description = "钱包编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long userId;

    @Schema(description = "用户类型，对应字典 user_type", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer userType;

    @Schema(description = "余额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer balance;

    @Schema(description = "累计支出，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    private Integer totalExpense;

    @Schema(description = "累计充值，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "300")
    private Integer totalRecharge;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
