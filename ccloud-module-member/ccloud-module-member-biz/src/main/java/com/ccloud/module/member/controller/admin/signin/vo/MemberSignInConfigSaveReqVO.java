package com.ccloud.module.member.controller.admin.signin.vo;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 签到规则创建/修改 Request VO")
@Data
public class MemberSignInConfigSaveReqVO {

    @Schema(description = "规则编号，修改时必填", example = "1")
    private Long id;

    @Schema(description = "连续签到第几天", requiredMode = Schema.RequiredMode.REQUIRED, example = "7")
    @NotNull(message = "签到天数不能为空")
    @Min(value = 1, message = "签到天数必须大于 0")
    private Integer day;

    @Schema(description = "奖励积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "14")
    @NotNull(message = "奖励积分不能为空")
    @Min(value = 0, message = "奖励积分不能小于 0")
    private Integer point;

    @Schema(description = "奖励经验", requiredMode = Schema.RequiredMode.REQUIRED, example = "7")
    @NotNull(message = "奖励经验不能为空")
    @Min(value = 0, message = "奖励经验不能小于 0")
    private Integer experience;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;
}
