package com.ccloud.module.member.controller.admin.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 会员修改积分 Request VO")
@Data
public class MemberUserUpdatePointReqVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "用户编号不能为空")
    private Long id;

    @Schema(description = "调整的积分，正数增加、负数扣减，调整后不能小于 0", requiredMode = Schema.RequiredMode.REQUIRED, example = "50")
    @NotNull(message = "调整的积分不能为空")
    private Integer point;
}
