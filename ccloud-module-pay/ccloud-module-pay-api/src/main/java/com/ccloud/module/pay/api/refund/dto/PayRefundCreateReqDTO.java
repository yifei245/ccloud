package com.ccloud.module.pay.api.refund.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - 创建退款单 Request DTO")
@Data
@Accessors(chain = true)
public class PayRefundCreateReqDTO {

    @Schema(description = "支付应用标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "shop")
    @NotBlank(message = "支付应用标识不能为空")
    private String appKey;

    @Schema(description = "商户订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER-20261012-0001")
    @NotBlank(message = "商户订单号不能为空")
    @Size(max = 64, message = "商户订单号长度不能超过 64 个字符")
    private String merchantOrderId;

    @Schema(description = "商户退款单号，同一应用内唯一，相同单号重复创建返回同一退款单", requiredMode = Schema.RequiredMode.REQUIRED, example = "REFUND-20261012-0001")
    @NotBlank(message = "商户退款单号不能为空")
    @Size(max = 64, message = "商户退款单号长度不能超过 64 个字符")
    private String merchantRefundId;

    @Schema(description = "退款金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "退款金额不能为空")
    @Min(value = 1, message = "退款金额必须大于 0")
    private Integer price;

    @Schema(description = "退款原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "商品缺货")
    @NotBlank(message = "退款原因不能为空")
    @Size(max = 256, message = "退款原因长度不能超过 256 个字符")
    private String reason;

    @Schema(description = "用户 IP", example = "127.0.0.1")
    private String userIp;
}
