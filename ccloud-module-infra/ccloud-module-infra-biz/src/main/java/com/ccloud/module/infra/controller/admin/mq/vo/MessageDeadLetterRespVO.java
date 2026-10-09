package com.ccloud.module.infra.controller.admin.mq.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 消息死信 Response VO")
@Data
public class MessageDeadLetterRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "租户编号，0 表示无租户（如 Stream 消费死信）", example = "1")
    private Long tenantId;

    @Schema(description = "消息编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String messageId;

    @Schema(description = "消息主题", requiredMode = Schema.RequiredMode.REQUIRED, example = "order.created")
    private String topic;

    @Schema(description = "消息体，JSON")
    private String payload;

    @Schema(description = "已重试次数", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Integer retryCount;

    @Schema(description = "最终失败原因")
    private String errorMsg;

    @Schema(description = "是否已人工处理", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean handled;

    @Schema(description = "处理时间")
    private LocalDateTime handledTime;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
