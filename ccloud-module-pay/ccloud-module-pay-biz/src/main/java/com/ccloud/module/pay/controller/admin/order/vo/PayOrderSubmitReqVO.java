package com.ccloud.module.pay.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Schema(description = "支付单提交支付 Request VO，会员端与开放接口复用")
@Data
@Accessors(chain = true)
public class PayOrderSubmitReqVO {

    @Schema(description = "渠道编码，见字典 pay_channel_code", requiredMode = Schema.RequiredMode.REQUIRED, example = "mock")
    @NotBlank(message = "渠道编码不能为空")
    private String channelCode;

    @Schema(description = "支付方式，聚合渠道下的具体方式", example = "wx_lite")
    @Size(max = 32, message = "支付方式长度不能超过 32 个字符")
    private String payMethod;

    @Schema(description = "渠道额外参数，如微信的 openid")
    private Map<String, String> channelExtras;
}
