package com.ccloud.module.pay.controller.admin.wallet.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 钱包充值套餐分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PayWalletRechargePackagePageReqVO extends PageParam {

    @Schema(description = "套餐名，模糊匹配", example = "充 100")
    private String name;

    @Schema(description = "状态，对应字典 common_status", example = "0")
    private Integer status;
}
