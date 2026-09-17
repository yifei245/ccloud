package com.ccloud.module.system.controller.admin.user.vo.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 个人中心 Response VO")
@Data
public class UserProfileRespVO {

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

    @Schema(description = "最后登录 IP", example = "192.168.1.1")
    private String loginIp;

    @Schema(description = "最后登录时间")
    private LocalDateTime loginDate;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "所属角色")
    private List<Item> roles;

    @Schema(description = "所在部门")
    private Item dept;

    @Schema(description = "所属岗位")
    private List<Item> posts;

    @Schema(description = "编号与名称")
    @Data
    public static class Item {

        @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Long id;

        @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "研发部")
        private String name;
    }
}
