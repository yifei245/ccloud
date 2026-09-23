package com.ccloud.module.system.controller.admin.notice.vo;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.module.system.enums.notice.NoticeTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 通知公告创建/修改 Request VO")
@Data
public class NoticeSaveReqVO {

    @Schema(description = "公告编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "公告标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "系统升级通知")
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 50, message = "公告标题长度不能超过 50 个字符")
    private String title;

    @Schema(description = "公告内容，富文本", requiredMode = Schema.RequiredMode.REQUIRED, example = "<p>今晚 22 点升级</p>")
    @NotBlank(message = "公告内容不能为空")
    private String content;

    @Schema(description = "类型：1 通知 2 公告", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "公告类型不能为空")
    @InEnum(value = NoticeTypeEnum.class, message = "公告类型必须是 {value}")
    private Integer type;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;
}
