package com.ccloud.module.open.controller.admin.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 开放应用创建 Response VO")
@Data
@ToString(exclude = "appSecret")
@Accessors(chain = true)
public class OpenAppCreateRespVO {

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "应用标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "a1b2c3d4e5f6g7h8")
    private String appKey;

    @Schema(description = "应用密钥，只在此返回一次，请妥善保存", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appSecret;
}
