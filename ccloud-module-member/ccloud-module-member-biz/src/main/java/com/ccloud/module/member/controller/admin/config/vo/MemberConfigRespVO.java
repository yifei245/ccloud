package com.ccloud.module.member.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 会员配置 Response VO")
@Data
public class MemberConfigRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "是否开启积分抵扣", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean pointTradeDeductEnable;

    @Schema(description = "1 积分抵扣多少分，单位：分", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer pointTradeDeductUnitPrice;

    @Schema(description = "单笔订单最多抵扣的积分数，为空表示不限", example = "10000")
    private Integer pointTradeDeductMaxPrice;

    @Schema(description = "每消费 1 元赠送的积分，为空表示不赠送", example = "1")
    private Long pointTradeGivePoint;
}
