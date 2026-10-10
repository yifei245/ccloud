package com.ccloud.module.open.api.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Schema(description = "RPC 服务 - 发送事件回调 Request DTO")
@Data
public class OpenNotifySendReqDTO {

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "应用编号不能为空")
    private Long appId;

    @Schema(description = "事件类型，见 OpenNotifyEvents", requiredMode = Schema.RequiredMode.REQUIRED, example = "open.test")
    @NotBlank(message = "事件类型不能为空")
    @Size(max = 64, message = "事件类型长度不能超过 64 个字符")
    private String event;

    @Schema(description = "事件数据", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "事件数据不能为空")
    private Map<String, Object> data;
}
