package com.ccloud.module.system.controller.admin.social.vo.client;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.module.system.enums.social.SocialTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 社交客户端创建/修改 Request VO")
@Data
public class SocialClientSaveReqVO {

    @Schema(description = "编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "应用名", requiredMode = Schema.RequiredMode.REQUIRED, example = "管理后台钉钉登录")
    @NotBlank(message = "应用名不能为空")
    @Size(max = 255, message = "应用名长度不能超过 255 个字符")
    private String name;

    @Schema(description = "社交平台，见 SocialTypeEnum", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @NotNull(message = "社交平台不能为空")
    @InEnum(SocialTypeEnum.class)
    private Integer socialType;

    @Schema(description = "用户类型：1 会员 2 管理员", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "用户类型不能为空")
    @InEnum(UserTypeEnum.class)
    private Integer userType;

    @Schema(description = "应用编号（appKey / appId）", requiredMode = Schema.RequiredMode.REQUIRED, example = "dingxxxxx")
    @NotBlank(message = "应用编号不能为空")
    @Size(max = 255, message = "应用编号长度不能超过 255 个字符")
    private String clientId;

    @Schema(description = "应用密钥；创建时必填，修改时留空表示不变", example = "secret")
    @Size(max = 128, message = "应用密钥长度不能超过 128 个字符")
    private String clientSecret;

    @Schema(description = "代理编号，企业微信必填", example = "1000002")
    @Size(max = 255, message = "代理编号长度不能超过 255 个字符")
    private String agentId;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;
}
