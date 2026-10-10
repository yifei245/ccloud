package com.ccloud.module.open.api.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "RPC 服务 - 开放应用快照 Response DTO")
@Data
public class OpenAppSnapshotRespDTO {

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "应用标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appKey;

    @Schema(description = "应用密钥原文，用于验签", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appSecret;

    @Schema(description = "应用所属租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long tenantId;

    @Schema(description = "状态，参见 CommonStatusEnum", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "过期时间，为空表示永不过期")
    private LocalDateTime expireTime;

    @Schema(description = "IP 白名单，逗号分隔，为空表示不限制")
    private String ipWhitelist;

    @Schema(description = "每秒请求数上限，为空表示不限流", example = "100")
    private Integer rateLimit;

    @Schema(description = "已授权且开启的接口", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Api> apis;

    @Schema(description = "已授权的接口")
    @Data
    public static class Api {

        @Schema(description = "请求方法", requiredMode = Schema.RequiredMode.REQUIRED, example = "GET")
        private String method;

        @Schema(description = "接口路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "/open-api/member/user/get")
        private String path;
    }
}
