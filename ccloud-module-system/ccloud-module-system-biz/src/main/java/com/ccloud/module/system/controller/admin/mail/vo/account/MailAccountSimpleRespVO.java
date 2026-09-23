package com.ccloud.module.system.controller.admin.mail.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 邮箱账号精简信息 Response VO")
@Data
public class MailAccountSimpleRespVO {

    @Schema(description = "账号编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "发件邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "noreply@ccloud.local")
    private String mail;
}
