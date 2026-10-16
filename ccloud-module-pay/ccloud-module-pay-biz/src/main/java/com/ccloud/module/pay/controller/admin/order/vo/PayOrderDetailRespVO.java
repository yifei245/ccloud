package com.ccloud.module.pay.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - 支付单详情 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class PayOrderDetailRespVO extends PayOrderRespVO {

    @Schema(description = "支付尝试列表，按发起顺序")
    private List<PayOrderExtensionRespVO> extensions;
}
