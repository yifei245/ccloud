package com.ccloud.module.system.controller.admin.sms.vo.template;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 短信模板分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SmsTemplatePageReqVO extends PageParam {

    @Schema(description = "模板类型", example = "1")
    private Integer type;

    @Schema(description = "状态：0 开启 1 关闭", example = "0")
    private Integer status;

    @Schema(description = "模板编码，模糊匹配", example = "login")
    private String code;

    @Schema(description = "模板内容，模糊匹配", example = "验证码")
    private String content;

    @Schema(description = "短信 API 的模板编号，模糊匹配", example = "SMS_")
    private String apiTemplateId;

    @Schema(description = "短信渠道编号", example = "1")
    private Long channelId;

    @Schema(description = "创建时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;
}
