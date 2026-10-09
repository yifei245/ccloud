package com.ccloud.module.member.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 会员配置保存 Request VO")
@Data
public class MemberConfigSaveReqVO {

    @Schema(description = "是否开启积分抵扣", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否开启积分抵扣不能为空")
    private Boolean pointTradeDeductEnable;

    @Schema(description = "1 积分抵扣多少分，单位：分", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "积分抵扣单价不能为空")
    @Min(value = 0, message = "积分抵扣单价不能小于 0")
    private Integer pointTradeDeductUnitPrice;

    @Schema(description = "单笔订单最多抵扣的积分数，不填表示不限", example = "10000")
    @Min(value = 0, message = "单笔最多抵扣积分不能小于 0")
    private Integer pointTradeDeductMaxPrice;

    @Schema(description = "每消费 1 元赠送的积分，不填表示不赠送", example = "1")
    @Min(value = 0, message = "赠送积分不能小于 0")
    private Long pointTradeGivePoint;
}
