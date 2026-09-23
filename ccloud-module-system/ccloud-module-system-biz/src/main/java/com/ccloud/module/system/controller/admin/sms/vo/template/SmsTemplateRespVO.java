package com.ccloud.module.system.controller.admin.sms.vo.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 短信模板 Response VO")
@Data
public class SmsTemplateRespVO {

    @Schema(description = "模板编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "模板类型：1 验证码 2 通知 3 营销", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer type;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "user-sms-login")
    private String code;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "会员-手机号登录")
    private String name;

    @Schema(description = "模板内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "您的验证码为 {code}")
    private String content;

    @Schema(description = "参数名数组", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"code\"]")
    private List<String> params;

    @Schema(description = "备注", example = "登录用")
    private String remark;

    @Schema(description = "短信 API 的模板编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SMS_207945135")
    private String apiTemplateId;

    @Schema(description = "短信渠道编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long channelId;

    @Schema(description = "短信渠道编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "ALIYUN")
    private String channelCode;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
