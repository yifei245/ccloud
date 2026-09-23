package com.ccloud.module.system.controller.admin.notify.vo.template;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 发送测试站内信 Request VO")
@Data
public class NotifyTemplateSendReqVO {

    @Schema(description = "接收用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @Schema(description = "接收用户类型：1 会员 2 管理员", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "用户类型不能为空")
    @InEnum(value = UserTypeEnum.class, message = "用户类型必须是 {value}")
    private Integer userType;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "bpm-approve")
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @Schema(description = "模板参数", requiredMode = Schema.RequiredMode.REQUIRED, example = "{\"no\": \"A001\"}")
    @NotNull(message = "模板参数不能为空")
    private Map<String, Object> templateParams;
}
