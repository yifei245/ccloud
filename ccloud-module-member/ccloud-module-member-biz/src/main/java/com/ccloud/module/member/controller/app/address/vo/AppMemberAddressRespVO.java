package com.ccloud.module.member.controller.app.address.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 收件地址 Response VO")
@Data
public class AppMemberAddressRespVO {

    @Schema(description = "地址编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "收件人名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String mobile;

    @Schema(description = "地区编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "440305")
    private Long areaId;

    @Schema(description = "详细地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "科技园 1 号")
    private String detailAddress;

    @Schema(description = "是否默认地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean defaultStatus;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
