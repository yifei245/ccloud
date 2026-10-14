package com.ccloud.module.pay.controller.admin.transfer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 发起转账 Request VO")
@Data
public class PayTransferCreateReqVO {

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "应用编号不能为空")
    private Long appId;

    @Schema(description = "渠道编码，对应字典 pay_channel_code", requiredMode = Schema.RequiredMode.REQUIRED, example = "wallet")
    @NotBlank(message = "渠道编码不能为空")
    private String channelCode;

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
}
