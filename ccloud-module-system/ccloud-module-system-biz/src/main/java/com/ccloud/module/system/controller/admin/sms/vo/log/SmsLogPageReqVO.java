package com.ccloud.module.system.controller.admin.sms.vo.log;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 短信日志分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SmsLogPageReqVO extends PageParam {

    @Schema(description = "短信渠道编号", example = "1")
    private Long channelId;

    @Schema(description = "模板编号", example = "1")
    private Long templateId;

    @Schema(description = "手机号，模糊匹配", example = "138")
    private String mobile;

    @Schema(description = "发送状态，见 SmsSendStatusEnum", example = "10")
    private Integer sendStatus;

    @Schema(description = "发送时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] sendTime;

    @Schema(description = "接收状态", example = "10")
    private Integer receiveStatus;

    @Schema(description = "接收时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] receiveTime;
}
