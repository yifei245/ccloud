package com.ccloud.module.system.controller.admin.sms.vo.template;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.module.system.enums.sms.SmsTemplateTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 短信模板创建/修改 Request VO")
@Data
public class SmsTemplateSaveReqVO {

    @Schema(description = "模板编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "模板类型：1 验证码 2 通知 3 营销", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "模板类型不能为空")
    @InEnum(value = SmsTemplateTypeEnum.class, message = "模板类型必须是 {value}")
    private Integer type;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "模板编码，业务按编码发送", requiredMode = Schema.RequiredMode.REQUIRED, example = "user-sms-login")
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 63, message = "模板编码长度不能超过 63 个字符")
    private String code;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "会员-手机号登录")
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 63, message = "模板名称长度不能超过 63 个字符")
    private String name;

    @Schema(description = "模板内容，参数写作 {参数名}", requiredMode = Schema.RequiredMode.REQUIRED, example = "您的验证码为 {code}")
    @NotBlank(message = "模板内容不能为空")
    @Size(max = 255, message = "模板内容长度不能超过 255 个字符")
    private String content;

    @Schema(description = "备注", example = "登录用")
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;

    @Schema(description = "短信 API 的模板编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SMS_207945135")
    @NotBlank(message = "短信 API 的模板编号不能为空")
    @Size(max = 63, message = "短信 API 的模板编号长度不能超过 63 个字符")
    private String apiTemplateId;

    @Schema(description = "短信渠道编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "短信渠道编号不能为空")
    private Long channelId;
}
