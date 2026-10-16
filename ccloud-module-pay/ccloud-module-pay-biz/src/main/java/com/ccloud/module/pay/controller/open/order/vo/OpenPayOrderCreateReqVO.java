package com.ccloud.module.pay.controller.open.order.vo;

import com.ccloud.module.pay.controller.open.json.OpenEpochMilliDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "开放接口 - 创建支付单 Request VO")
@Data
@Accessors(chain = true)
public class OpenPayOrderCreateReqVO {

    @Schema(description = "商户订单号，应用内唯一；重复创建返回原单", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER-20261012-0001")
    @NotBlank(message = "商户订单号不能为空")
    @Size(max = 64, message = "商户订单号长度不能超过 64 个字符")
    private String merchantOrderId;

    @Schema(description = "商品标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "测试商品")
    @NotBlank(message = "商品标题不能为空")
    @Size(max = 32, message = "商品标题长度不能超过 32 个字符")
    private String subject;

    @Schema(description = "商品描述", example = "商品详情")
    @Size(max = 128, message = "商品描述长度不能超过 128 个字符")
    private String body;

    @Schema(description = "支付金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "支付金额不能为空")
    @Min(value = 1, message = "支付金额必须大于 0")
    private Integer price;

    @Schema(description = "过期时间。毫秒时间戳（整数），精度为秒（毫秒部分忽略），须晚于当前时间且不晚于 2099-12-31 23:59:59；不接受 ISO 字符串、带引号的数字或数组",
            type = "integer", format = "int64", requiredMode = Schema.RequiredMode.REQUIRED, example = "1791000000000")
    @NotNull(message = "过期时间不能为空")
    @Future(message = "过期时间必须晚于当前时间")
    @JsonDeserialize(using = OpenEpochMilliDeserializer.class)
    private LocalDateTime expireTime;
}
