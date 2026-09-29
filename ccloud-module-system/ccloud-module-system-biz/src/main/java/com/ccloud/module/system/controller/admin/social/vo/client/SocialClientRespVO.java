package com.ccloud.module.system.controller.admin.social.vo.client;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 不含应用密钥：密钥只写不读
 */
@Schema(description = "管理后台 - 社交客户端 Response VO")
@Data
public class SocialClientRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "应用名", requiredMode = Schema.RequiredMode.REQUIRED, example = "管理后台钉钉登录")
    private String name;

    @Schema(description = "社交平台", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    private Integer socialType;

    @Schema(description = "用户类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer userType;

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "dingxxxxx")
    private String clientId;

    @Schema(description = "代理编号", example = "1000002")
    private String agentId;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
