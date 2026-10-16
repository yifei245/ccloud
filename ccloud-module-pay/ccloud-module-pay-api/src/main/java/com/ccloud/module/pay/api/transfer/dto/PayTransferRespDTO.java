package com.ccloud.module.pay.api.transfer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - 转账单 Response DTO")
@Data
@Accessors(chain = true)
public class PayTransferRespDTO {

    @Schema(description = "转账单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    private Long id;

    @Schema(description = "商户转账单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRANSFER-20261012-0001")
    private String merchantTransferId;

    @Schema(description = "转账金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer price;

    @Schema(description = "转账状态，见字典 pay_transfer_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "转账成功时间", example = "2026-10-12 15:30:00")
    private LocalDateTime successTime;

    @Schema(description = "渠道返回的错误描述", example = "收款账号不存在")
    private String channelErrorMsg;
}
