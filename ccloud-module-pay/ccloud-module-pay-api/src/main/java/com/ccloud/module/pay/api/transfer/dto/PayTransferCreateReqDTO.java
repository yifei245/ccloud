package com.ccloud.module.pay.api.transfer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - 创建转账单 Request DTO")
@Data
@Accessors(chain = true)
public class PayTransferCreateReqDTO {

    @Schema(description = "支付应用标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "shop")
    @NotBlank(message = "支付应用标识不能为空")
    private String appKey;

    @Schema(description = "渠道编码，见字典 pay_channel_code", requiredMode = Schema.RequiredMode.REQUIRED, example = "wallet")
    @NotBlank(message = "渠道编码不能为空")
    private String channelCode;

    @Schema(description = "商户转账单号，同一应用内唯一，相同单号重复创建返回同一转账单", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRANSFER-20261012-0001")
    @NotBlank(message = "商户转账单号不能为空")
    @Size(max = 64, message = "商户转账单号长度不能超过 64 个字符")
    private String merchantTransferId;

    @Schema(description = "转账标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "推广佣金")
    @NotBlank(message = "转账标题不能为空")
    @Size(max = 512, message = "转账标题长度不能超过 512 个字符")
    private String subject;

    @Schema(description = "转账金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "转账金额不能为空")
    @Min(value = 1, message = "转账金额必须大于 0")
    private Integer price;

    @Schema(description = "收款账号，钱包转账时为会员编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotBlank(message = "收款账号不能为空")
    @Size(max = 64, message = "收款账号长度不能超过 64 个字符")
    private String userAccount;

    @Schema(description = "收款人姓名", example = "张三")
    @Size(max = 64, message = "收款人姓名长度不能超过 64 个字符")
    private String userName;

    @Schema(description = "用户 IP", requiredMode = Schema.RequiredMode.REQUIRED, example = "127.0.0.1")
    @NotBlank(message = "用户 IP 不能为空")
    private String userIp;
}
