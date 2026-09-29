package com.ccloud.module.member.controller.app.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

@Schema(description = "用户 App - 登录用户信息 Response VO。C 端没有菜单树与权限标识，只返回用户与功能开关")
@Data
public class AppAuthPermissionInfoRespVO {

    @Schema(description = "用户信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserVO user;

    @Schema(description = "已开通的功能，前端据此显示入口", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "[\"point\", \"sign-in\"]")
    private Set<String> features;

    @Schema(description = "用户信息")
    @Data
    public static class UserVO {

        @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Long id;

        @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
        private String nickname;

        @Schema(description = "头像", example = "https://www.ccloud.local/avatar.png")
        private String avatar;

        @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
        private String mobile;

        @Schema(description = "积分", example = "100")
        private Integer point;

        @Schema(description = "经验值", example = "200")
        private Integer experience;

        @Schema(description = "等级编号", example = "1")
        private Long levelId;
    }
}
