package com.ccloud.module.system.controller.admin.mail.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 不含密码：密码只写不读
 */
@Schema(description = "管理后台 - 邮箱账号 Response VO")
@Data
public class MailAccountRespVO {

    @Schema(description = "账号编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "发件邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "noreply@ccloud.local")
    private String mail;

    @Schema(description = "SMTP 用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "noreply@ccloud.local")
    private String username;

    @Schema(description = "SMTP 服务器域名", requiredMode = Schema.RequiredMode.REQUIRED, example = "smtp.qq.com")
    private String host;

    @Schema(description = "SMTP 服务器端口", requiredMode = Schema.RequiredMode.REQUIRED, example = "465")
    private Integer port;

    @Schema(description = "是否开启 SSL", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean sslEnable;

    @Schema(description = "是否开启 STARTTLS", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean starttlsEnable;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
