package com.ccloud.module.system.controller.admin.sms.vo.template;

import com.ccloud.framework.common.validation.Mobile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 发送测试短信 Request VO")
@Data
public class SmsTemplateSendReqVO {

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800000000")
    @NotBlank(message = "手机号不能为空")
    @Mobile
    private String mobile;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "user-sms-login")
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @Schema(description = "模板参数", requiredMode = Schema.RequiredMode.REQUIRED, example = "{\"code\": \"1234\"}")
    @NotNull(message = "模板参数不能为空")
    private Map<String, Object> templateParams;
}
