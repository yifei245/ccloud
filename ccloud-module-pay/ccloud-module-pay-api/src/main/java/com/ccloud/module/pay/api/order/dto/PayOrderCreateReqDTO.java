package com.ccloud.module.pay.api.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - 创建支付单 Request DTO")
@Data
@Accessors(chain = true)
public class PayOrderCreateReqDTO {

    @Schema(description = "支付应用标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "shop")
    @NotBlank(message = "支付应用标识不能为空")
    private String appKey;

    @Schema(description = "商户订单号，同一应用内唯一，相同单号重复创建返回同一支付单", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER-20261012-0001")
    @NotBlank(message = "商户订单号不能为空")
    @Size(max = 64, message = "商户订单号长度不能超过 64 个字符")
    private String merchantOrderId;

    @Schema(description = "商品标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "会员月卡")
    @NotBlank(message = "商品标题不能为空")
    @Size(max = 32, message = "商品标题长度不能超过 32 个字符")
    private String subject;

    @Schema(description = "商品描述", example = "会员月卡 30 天")
    @Size(max = 128, message = "商品描述长度不能超过 128 个字符")
    private String body;

    @Schema(description = "支付金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "支付金额不能为空")
    @Min(value = 1, message = "支付金额必须大于 0")
    private Integer price;

    @Schema(description = "支付过期时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-12 18:00:00")
    @NotNull(message = "支付过期时间不能为空")
    @Future(message = "支付过期时间必须是将来时间")
    private LocalDateTime expireTime;

    @Schema(description = "用户 IP", requiredMode = Schema.RequiredMode.REQUIRED, example = "127.0.0.1")
    @NotBlank(message = "用户 IP 不能为空")
    private String userIp;

    @Schema(description = "付款人编号，会员订单必填，外部订单为空", example = "1024")
    private Long userId;

    @Schema(description = "付款人类型，会员订单必填，外部订单为空", example = "1")
    private Integer userType;
}
