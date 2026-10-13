package com.ccloud.module.open.api.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Schema(description = "RPC 服务 - 发送事件回调 Request DTO")
@Data
public class OpenNotifySendReqDTO {

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "应用编号不能为空")
    private Long appId;

    @Schema(description = "事件编号，供调用方去重：相同编号重复发送只记一条回调。为空时自动生成", example = "5f0c2a9e1b7d4c3e8a6f9b2d1c0e7a54")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$", message = "事件编号只能包含字母、数字、下划线与中划线，最长 64 位")
    private String eventId;

    @Schema(description = "事件类型，见 OpenNotifyEvents", requiredMode = Schema.RequiredMode.REQUIRED, example = "open.test")
    @NotBlank(message = "事件类型不能为空")
    @Size(max = 64, message = "事件类型长度不能超过 64 个字符")
    private String event;

    @Schema(description = "事件数据", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "事件数据不能为空")
    private Map<String, Object> data;
}
