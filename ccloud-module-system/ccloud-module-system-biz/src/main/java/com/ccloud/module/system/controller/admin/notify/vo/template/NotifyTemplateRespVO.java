package com.ccloud.module.system.controller.admin.notify.vo.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 站内信模板 Response VO")
@Data
public class NotifyTemplateRespVO {

    @Schema(description = "模板编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "审批通过")
    private String name;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "bpm-approve")
    private String code;

    @Schema(description = "发送人名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "系统")
    private String nickname;

    @Schema(description = "模板内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "您的申请 {no} 已审批通过")
    private String content;

    @Schema(description = "类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer type;

    @Schema(description = "参数名数组", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"no\"]")
    private List<String> params;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "备注", example = "工作流审批通过时发送")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
