package com.ccloud.module.member.controller.admin.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 会员修改等级 Request VO")
@Data
public class MemberUserUpdateLevelReqVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "用户编号不能为空")
    private Long id;

    @Schema(description = "等级编号，须为启用的等级；会员经验同步调到该等级的门槛", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "等级不能为空")
    private Long levelId;

    @Schema(description = "修改原因，记入等级变更记录", requiredMode = Schema.RequiredMode.REQUIRED, example = "活动赠送")
    @NotBlank(message = "修改原因不能为空")
    @Size(max = 255, message = "修改原因长度不能超过 255 个字符")
    private String reason;
}
