package com.ccloud.module.system.controller.admin.mail.vo.log;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 邮件日志 Response VO")
@Data
public class MailLogRespVO {

    @Schema(description = "日志编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "用户编号", example = "1")
    private Long userId;

    @Schema(description = "用户类型", example = "2")
    private Integer userType;

    @Schema(description = "收件邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "user@ccloud.local")
    private String toMail;

    @Schema(description = "发件邮箱账号编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long accountId;

    @Schema(description = "发件邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "noreply@ccloud.local")
    private String fromMail;

    @Schema(description = "模板编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long templateId;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "register-success")
    private String templateCode;

    @Schema(description = "发件人名称", example = "ccloud")
    private String templateNickname;

    @Schema(description = "邮件标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "欢迎张三")
    private String templateTitle;

    @Schema(description = "邮件内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "<p>张三，您好</p>")
    private String templateContent;

    @Schema(description = "邮件参数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> templateParams;

    @Schema(description = "发送状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer sendStatus;

    @Schema(description = "发送时间")
    private LocalDateTime sendTime;

    @Schema(description = "发送返回的 Message-ID", example = "<1.2@ccloud.local>")
    private String sendMessageId;

    @Schema(description = "发送异常")
    private String sendException;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
