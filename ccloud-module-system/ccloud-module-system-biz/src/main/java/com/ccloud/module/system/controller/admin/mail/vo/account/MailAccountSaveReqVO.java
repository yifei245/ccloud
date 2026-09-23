package com.ccloud.module.system.controller.admin.mail.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Schema(description = "管理后台 - 邮箱账号创建/修改 Request VO")
@Data
public class MailAccountSaveReqVO {

    @Schema(description = "账号编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "发件邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "noreply@ccloud.local")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱长度不能超过 255 个字符")
    private String mail;

    @Schema(description = "SMTP 用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "noreply@ccloud.local")
    @NotBlank(message = "用户名不能为空")
    @Size(max = 255, message = "用户名长度不能超过 255 个字符")
    private String username;

    @Schema(description = "SMTP 密码或授权码；创建时必填，修改时留空表示不变", example = "password")
    @Size(max = 64, message = "密码长度不能超过 64 个字符")
    private String password;

    @Schema(description = "SMTP 服务器域名", requiredMode = Schema.RequiredMode.REQUIRED, example = "smtp.qq.com")
    @NotBlank(message = "SMTP 服务器域名不能为空")
    @Size(max = 255, message = "SMTP 服务器域名长度不能超过 255 个字符")
    private String host;

    @Schema(description = "SMTP 服务器端口", requiredMode = Schema.RequiredMode.REQUIRED, example = "465")
    @NotNull(message = "SMTP 服务器端口不能为空")
    @Min(value = 1, message = "端口范围为 1-65535")
    @Max(value = 65535, message = "端口范围为 1-65535")
    private Integer port;

    @Schema(description = "是否开启 SSL", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否开启 SSL 不能为空")
    private Boolean sslEnable;

    @Schema(description = "是否开启 STARTTLS", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    @NotNull(message = "是否开启 STARTTLS 不能为空")
    private Boolean starttlsEnable;
}
