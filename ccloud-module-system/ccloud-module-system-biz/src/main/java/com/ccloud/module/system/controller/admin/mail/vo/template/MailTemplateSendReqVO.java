package com.ccloud.module.system.controller.admin.mail.vo.template;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 发送测试邮件 Request VO")
@Data
public class MailTemplateSendReqVO {

    @Schema(description = "收件邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "user@ccloud.local")
    @NotBlank(message = "收件邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String mail;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "register-success")
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @Schema(description = "模板参数", requiredMode = Schema.RequiredMode.REQUIRED, example = "{\"name\": \"张三\"}")
    @NotNull(message = "模板参数不能为空")
    private Map<String, Object> templateParams;
}
