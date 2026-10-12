package com.ccloud.module.open.controller.admin.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 开放应用 Response VO")
@Data
@ToString(exclude = "appSecret")
@Accessors(chain = true)
public class OpenAppRespVO {

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "合作方 A")
    private String name;

    @Schema(description = "应用标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "a1b2c3d4e5f6g7h8")
    private String appKey;

    @Schema(description = "应用密钥，已掩码，只显示前后各 4 位", requiredMode = Schema.RequiredMode.REQUIRED, example = "abcd****3456")
    private String appSecret;

    @Schema(description = "状态，对应字典 common_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "IP 白名单，为空表示不限制")
    private String ipWhitelist;

    @Schema(description = "每秒请求上限", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer rateLimit;

    @Schema(description = "过期时间，为空表示长期有效")
    private LocalDateTime expireTime;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactMobile;

    @Schema(description = "事件回调地址")
    private String notifyUrl;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
