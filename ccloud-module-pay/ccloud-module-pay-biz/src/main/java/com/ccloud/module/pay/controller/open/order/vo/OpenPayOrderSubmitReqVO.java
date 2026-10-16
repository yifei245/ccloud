package com.ccloud.module.pay.controller.open.order.vo;

import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@Schema(description = "开放接口 - 提交支付 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class OpenPayOrderSubmitReqVO extends PayOrderSubmitReqVO {

    @Schema(description = "商户订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER-20261012-0001")
    @NotBlank(message = "商户订单号不能为空")
    private String merchantOrderId;
}
