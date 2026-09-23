package com.ccloud.module.system.controller.admin.mail.vo.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 邮件模板 Response VO")
@Data
public class MailTemplateRespVO {

    @Schema(description = "模板编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "注册成功")
    private String name;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "register-success")
    private String code;

    @Schema(description = "发件邮箱账号编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long accountId;

    @Schema(description = "发件人名称", example = "ccloud")
    private String nickname;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "欢迎 {name}")
    private String title;

    @Schema(description = "内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "<p>{name}，您好</p>")
    private String content;

    @Schema(description = "参数名数组", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"name\"]")
    private List<String> params;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "备注", example = "注册成功后发送")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
