package com.ccloud.module.member.controller.open.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "开放接口 - 会员基本信息 Response VO")
@Data
@Accessors(chain = true)
public class OpenMemberUserRespVO {

    @Schema(description = "会员编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    private String nickname;

    @Schema(description = "头像", example = "https://www.ccloud.local/avatar.png")
    private String avatar;

    @Schema(description = "手机号（脱敏）", requiredMode = Schema.RequiredMode.REQUIRED, example = "138****8000")
    private String mobile;

    @Schema(description = "会员等级名称", example = "黄金会员")
    private String levelName;

    @Schema(description = "积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer point;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;
}
