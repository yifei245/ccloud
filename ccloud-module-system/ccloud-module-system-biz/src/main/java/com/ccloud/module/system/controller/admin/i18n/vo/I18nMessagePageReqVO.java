package com.ccloud.module.system.controller.admin.i18n.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 国际化文案分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class I18nMessagePageReqVO extends PageParam {

    @Schema(description = "语言", example = "zh_CN")
    private String locale;

    @Schema(description = "文案编码，模糊匹配", example = "error.code")
    private String code;

    @Schema(description = "文案内容，模糊匹配", example = "登录")
    private String content;

    @Schema(description = "来源：1 系统文案 2 业务文案", example = "1")
    private Integer source;
}
