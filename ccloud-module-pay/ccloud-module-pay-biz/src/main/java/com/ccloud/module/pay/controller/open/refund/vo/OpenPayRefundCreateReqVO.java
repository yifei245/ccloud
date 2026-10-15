package com.ccloud.module.pay.controller.open.refund.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "开放接口 - 发起退款 Request VO")
@Data
@Accessors(chain = true)
public class OpenPayRefundCreateReqVO {

    @Schema(description = "商户订单号，须是本应用的订单", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER-20261012-0001")
    @NotBlank(message = "商户订单号不能为空")
    @Size(max = 64, message = "商户订单号长度不能超过 64 个字符")
    private String merchantOrderId;

    @Schema(description = "商户退款单号，应用内唯一；重复发起返回原退款单", requiredMode = Schema.RequiredMode.REQUIRED, example = "REFUND-20261012-0001")
    @NotBlank(message = "商户退款单号不能为空")
    @Size(max = 64, message = "商户退款单号长度不能超过 64 个字符")
    private String merchantRefundId;

    @Schema(description = "退款金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    @NotNull(message = "退款金额不能为空")
    @Min(value = 1, message = "退款金额必须大于 0")
    private Integer price;

    @Schema(description = "退款原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "商品缺货")
    @NotBlank(message = "退款原因不能为空")
    @Size(max = 256, message = "退款原因长度不能超过 256 个字符")
    private String reason;
}
