package com.ccloud.module.system.controller.admin.oauth2.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "OAuth2 开放接口 - 用户信息 Response VO")
@Data
public class OAuth2UserInfoRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    private String username;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "管理员")
    private String nickname;

    @Schema(description = "用户邮箱", example = "admin@ccloud.local")
    private String email;

    @Schema(description = "手机号码", example = "13800138000")
    private String mobile;

    @Schema(description = "用户性别：0 未知 1 男 2 女", example = "1")
    private Integer sex;

    @Schema(description = "用户头像", example = "https://www.ccloud.local/avatar.png")
    private String avatar;
}
