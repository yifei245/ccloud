package com.ccloud.module.system.controller.admin.sms.vo.channel;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 短信渠道 Response VO")
@Data
public class SmsChannelRespVO {

    @Schema(description = "渠道编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "短信签名", requiredMode = Schema.RequiredMode.REQUIRED, example = "ccloud")
    private String signature;

    @Schema(description = "渠道编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "ALIYUN")
    private String code;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "备注", example = "主渠道")
    private String remark;

    @Schema(description = "短信 API 账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "LTAI5t")
    private String apiKey;

    @Schema(description = "短信 API 密钥", example = "secret")
    private String apiSecret;

    @Schema(description = "短信发送回调地址", example = "https://www.ccloud.local/admin-api/system/sms/callback/aliyun")
    private String callbackUrl;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
