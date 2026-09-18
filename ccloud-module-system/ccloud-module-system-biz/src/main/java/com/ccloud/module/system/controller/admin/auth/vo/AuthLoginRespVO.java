package com.ccloud.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 登录 Response VO")
@Data
public class AuthLoginRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "访问令牌", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "e1b0a9f3c6d54ef1a6b0a1c2d3e4f5a6")
    private String accessToken;

    @Schema(description = "刷新令牌", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "0a1b2c3d4e5f67890a1b2c3d4e5f6789")
    private String refreshToken;

    @Schema(description = "过期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expiresTime;
}
