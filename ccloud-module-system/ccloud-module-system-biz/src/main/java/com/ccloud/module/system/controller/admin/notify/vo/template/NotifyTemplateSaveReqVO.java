package com.ccloud.module.system.controller.admin.notify.vo.template;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.module.system.enums.notify.NotifyTemplateTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 站内信模板创建/修改 Request VO")
@Data
public class NotifyTemplateSaveReqVO {

    @Schema(description = "模板编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "审批通过")
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 63, message = "模板名称长度不能超过 63 个字符")
    private String name;

    @Schema(description = "模板编码，业务按编码发送", requiredMode = Schema.RequiredMode.REQUIRED, example = "bpm-approve")
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 64, message = "模板编码长度不能超过 64 个字符")
    private String code;

    @Schema(description = "发送人名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "系统")
    @NotBlank(message = "发送人名称不能为空")
    @Size(max = 63, message = "发送人名称长度不能超过 63 个字符")
    private String nickname;

    @Schema(description = "模板内容，参数写作 {参数名}", requiredMode = Schema.RequiredMode.REQUIRED, example = "您的申请 {no} 已审批通过")
    @NotBlank(message = "模板内容不能为空")
    @Size(max = 1024, message = "模板内容长度不能超过 1024 个字符")
    private String content;

    @Schema(description = "类型：1 通知公告 2 系统消息", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "类型不能为空")
    @InEnum(value = NotifyTemplateTypeEnum.class, message = "类型必须是 {value}")
    private Integer type;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "备注", example = "工作流审批通过时发送")
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;
}
