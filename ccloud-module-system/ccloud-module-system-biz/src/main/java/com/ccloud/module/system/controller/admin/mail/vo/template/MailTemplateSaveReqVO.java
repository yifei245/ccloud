package com.ccloud.module.system.controller.admin.mail.vo.template;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 邮件模板创建/修改 Request VO")
@Data
public class MailTemplateSaveReqVO {

    @Schema(description = "模板编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "注册成功")
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 63, message = "模板名称长度不能超过 63 个字符")
    private String name;

    @Schema(description = "模板编码，业务按编码发送", requiredMode = Schema.RequiredMode.REQUIRED, example = "register-success")
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 63, message = "模板编码长度不能超过 63 个字符")
    private String code;

    @Schema(description = "发件邮箱账号编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "发件邮箱账号不能为空")
    private Long accountId;

    @Schema(description = "发件人名称", example = "ccloud")
    @Size(max = 255, message = "发件人名称长度不能超过 255 个字符")
    private String nickname;

    @Schema(description = "标题，参数写作 {参数名}", requiredMode = Schema.RequiredMode.REQUIRED, example = "欢迎 {name}")
    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过 255 个字符")
    private String title;

    @Schema(description = "内容，HTML，参数写作 {参数名}", requiredMode = Schema.RequiredMode.REQUIRED, example = "<p>{name}，您好</p>")
    @NotBlank(message = "内容不能为空")
    @Size(max = 10240, message = "内容长度不能超过 10240 个字符")
    private String content;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "备注", example = "注册成功后发送")
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;
}
