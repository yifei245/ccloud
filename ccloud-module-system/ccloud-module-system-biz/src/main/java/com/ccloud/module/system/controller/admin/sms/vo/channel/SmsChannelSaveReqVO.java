package com.ccloud.module.system.controller.admin.sms.vo.channel;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Schema(description = "管理后台 - 短信渠道创建/修改 Request VO")
@Data
public class SmsChannelSaveReqVO {

    @Schema(description = "渠道编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "短信签名", requiredMode = Schema.RequiredMode.REQUIRED, example = "ccloud")
    @NotBlank(message = "短信签名不能为空")
    @Size(max = 12, message = "短信签名长度不能超过 12 个字符")
    private String signature;

    @Schema(description = "渠道编码，见 SmsChannelEnum", requiredMode = Schema.RequiredMode.REQUIRED, example = "ALIYUN")
    @NotBlank(message = "渠道编码不能为空")
    private String code;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "备注", example = "主渠道")
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;

    @Schema(description = "短信 API 账号；腾讯云为「SecretId SdkAppId」", requiredMode = Schema.RequiredMode.REQUIRED, example = "LTAI5t")
    @NotBlank(message = "短信 API 账号不能为空")
    @Size(max = 128, message = "短信 API 账号长度不能超过 128 个字符")
    private String apiKey;

    @Schema(description = "短信 API 密钥", example = "secret")
    @Size(max = 128, message = "短信 API 密钥长度不能超过 128 个字符")
    private String apiSecret;

    @Schema(description = "短信发送回调地址", example = "https://www.ccloud.local/admin-api/system/sms/callback/aliyun")
    @URL(message = "回调地址格式不正确")
    @Size(max = 255, message = "回调地址长度不能超过 255 个字符")
    private String callbackUrl;
}
