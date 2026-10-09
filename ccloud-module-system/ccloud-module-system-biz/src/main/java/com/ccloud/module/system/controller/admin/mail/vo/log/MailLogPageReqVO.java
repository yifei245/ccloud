package com.ccloud.module.system.controller.admin.mail.vo.log;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 邮件日志分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MailLogPageReqVO extends PageParam {

    @Schema(description = "用户编号", example = "1")
    private Long userId;

    @Schema(description = "用户类型", example = "2")
    private Integer userType;

    @Schema(description = "收件邮箱，模糊匹配", example = "user@")
    private String toMail;

    @Schema(description = "发件邮箱账号编号", example = "1")
    private Long accountId;

    @Schema(description = "模板编号", example = "1")
    private Long templateId;

    @Schema(description = "发送状态，见 MailSendStatusEnum", example = "10")
    private Integer sendStatus;

    @Schema(description = "发送时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] sendTime;
}
