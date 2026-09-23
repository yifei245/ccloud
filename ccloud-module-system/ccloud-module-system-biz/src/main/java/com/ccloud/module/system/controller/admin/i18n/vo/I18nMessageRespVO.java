package com.ccloud.module.system.controller.admin.i18n.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 国际化文案 Response VO")
@Data
public class I18nMessageRespVO {

    @Schema(description = "文案编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "语言", requiredMode = Schema.RequiredMode.REQUIRED, example = "zh_CN")
    private String locale;

    @Schema(description = "文案编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "error.code.1001000000")
    private String code;

    @Schema(description = "文案内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "登录失败，账号密码不正确")
    private String content;

    @Schema(description = "来源：1 系统文案 2 业务文案", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer source;

    @Schema(description = "业务类型", example = "product_spu")
    private String bizType;

    @Schema(description = "业务编号", example = "1")
    private Long bizId;

    @Schema(description = "业务字段", example = "name")
    private String field;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;
}
