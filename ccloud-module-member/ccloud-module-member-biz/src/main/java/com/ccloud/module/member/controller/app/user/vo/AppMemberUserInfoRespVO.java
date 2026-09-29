package com.ccloud.module.member.controller.app.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 会员用户信息 Response VO")
@Data
public class AppMemberUserInfoRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    private String nickname;

    @Schema(description = "头像", example = "https://www.ccloud.local/avatar.png")
    private String avatar;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String mobile;

    @Schema(description = "性别：0 未知 1 男 2 女", example = "1")
    private Integer sex;

    @Schema(description = "生日")
    private LocalDateTime birthday;

    @Schema(description = "积分", example = "100")
    private Integer point;

    @Schema(description = "经验值", example = "200")
    private Integer experience;

    @Schema(description = "是否已设置密码。短信或小程序注册的用户没有密码，前端据此提示设置")
    private Boolean passwordSet;
}
