package com.ccloud.module.open.controller.admin.notify.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 开放应用回调日志 Response VO")
@Data
public class OpenNotifyLogRespVO {

    @Schema(description = "日志编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "租户编号", example = "1")
    private Long tenantId;

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long appId;

    @Schema(description = "事件编号，回调时放入 X-Open-Event-Id", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "3f2b8c1d9e7a4b6c8d0e1f2a3b4c5d6e")
    private String eventId;

    @Schema(description = "事件类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "open.test")
    private String event;

    @Schema(description = "回调请求体，JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    private String payload;

    @Schema(description = "回调地址，应用未配置时为空串", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "https://example.com/notify")
    private String notifyUrl;

    @Schema(description = "状态，对应字典 open_notify_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "已重试次数", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer retryCount;

    @Schema(description = "下次重试时间")
    private LocalDateTime nextRetryTime;

    @Schema(description = "最近一次通知时间")
    private LocalDateTime lastNotifyTime;

    @Schema(description = "最近一次对方返回的 HTTP 状态码，请求未发出时为 0", example = "200")
    private Integer httpStatus;

    @Schema(description = "对方最近一次响应内容（截断到 2048 字符），未发出时为失败原因", example = "success")
    private String responseBody;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
