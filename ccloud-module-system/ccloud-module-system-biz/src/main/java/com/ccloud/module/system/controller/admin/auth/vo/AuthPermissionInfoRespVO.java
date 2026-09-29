package com.ccloud.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 前端据此渲染左侧菜单（menus）与按钮级权限（permissions）
 */
@Schema(description = "管理后台 - 登录用户的权限信息 Response VO")
@Data
public class AuthPermissionInfoRespVO {

    @Schema(description = "用户信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserVO user;

    @Schema(description = "角色标识集合", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"super_admin\"]")
    private Set<String> roles;

    @Schema(description = "权限标识集合，按钮级控制的依据", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "[\"system:user:create\"]")
    private Set<String> permissions;

    @Schema(description = "菜单树，仅含目录与菜单", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<MenuVO> menus;

    @Schema(description = "用户信息")
    @Data
    public static class UserVO {

        @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Long id;

        @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "管理员")
        private String nickname;

        @Schema(description = "用户头像", example = "https://www.ccloud.local/avatar.png")
        private String avatar;

        @Schema(description = "部门编号", example = "100")
        private Long deptId;
    }

    @Schema(description = "菜单")
    @Data
    public static class MenuVO {

        @Schema(description = "菜单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        private Long id;

        @Schema(description = "父菜单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
        private Long parentId;

        @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "用户管理")
        private String name;

        @Schema(description = "路由地址", example = "user")
        private String path;

        @Schema(description = "组件路径", example = "system/user/index")
        private String component;

        @Schema(description = "组件名", example = "SystemUser")
        private String componentName;

        @Schema(description = "菜单图标", example = "ep:user")
        private String icon;

        @Schema(description = "是否可见", example = "true")
        private Boolean visible;

        @Schema(description = "是否缓存", example = "true")
        private Boolean keepAlive;

        @Schema(description = "是否总是显示", example = "false")
        private Boolean alwaysShow;

        @Schema(description = "子菜单")
        private List<MenuVO> children;
    }
}
