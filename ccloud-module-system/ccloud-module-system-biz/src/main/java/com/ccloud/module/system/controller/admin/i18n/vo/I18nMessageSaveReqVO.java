package com.ccloud.module.system.controller.admin.i18n.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 国际化文案创建/修改 Request VO")
@Data
public class I18nMessageSaveReqVO {

    @Schema(description = "文案编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "语言，须为服务支持的语言", requiredMode = Schema.RequiredMode.REQUIRED, example = "zh_CN")
    @NotBlank(message = "语言不能为空")
    @Size(max = 16, message = "语言长度不能超过 16 个字符")
    private String locale;

    @Schema(description = "文案编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "error.code.1001000000")
    @NotBlank(message = "文案编码不能为空")
    @Size(max = 255, message = "文案编码长度不能超过 255 个字符")
    private String code;

    @Schema(description = "文案内容，占位符写作 {0}、{1}", requiredMode = Schema.RequiredMode.REQUIRED, example = "登录失败，账号密码不正确")
    @NotBlank(message = "文案内容不能为空")
    @Size(max = 2048, message = "文案内容长度不能超过 2048 个字符")
    private String content;
}
