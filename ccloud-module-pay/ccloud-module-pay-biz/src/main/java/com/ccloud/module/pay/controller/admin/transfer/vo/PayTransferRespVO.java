package com.ccloud.module.pay.controller.admin.transfer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 转账单 Response VO")
@Data
@Accessors(chain = true)
public class PayTransferRespVO {

    @Schema(description = "转账单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    private Long id;

    @Schema(description = "外部转账单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "T20261012153000000001")
    private String no;

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long appId;

    @Schema(description = "渠道编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Long channelId;

    @Schema(description = "渠道编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "wallet")
    private String channelCode;

    @Schema(description = "商户转账单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRANSFER-20261012-0001")
    private String merchantTransferId;

    @Schema(description = "转账标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "推广佣金")
    private String subject;

    @Schema(description = "转账金额，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer price;

    @Schema(description = "收款账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private String userAccount;

    @Schema(description = "收款人姓名", example = "张三")
    private String userName;

    @Schema(description = "转账状态，对应字典 pay_transfer_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "转账成功时间")
    private LocalDateTime successTime;

    @Schema(description = "用户 IP", example = "127.0.0.1")
    private String userIp;

    @Schema(description = "渠道转账单号", example = "50000123")
    private String channelTransferNo;

    @Schema(description = "渠道错误码", example = "MOCK_FAIL")
    private String channelErrorCode;

    @Schema(description = "渠道错误信息", example = "收款账号不存在")
    private String channelErrorMsg;

    @Schema(description = "渠道回调原始报文，供排查")
    private String channelNotifyData;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
