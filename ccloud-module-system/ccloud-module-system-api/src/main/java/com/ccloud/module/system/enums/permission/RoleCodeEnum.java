package com.ccloud.module.system.enums.permission;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 内置角色编码
 */
@Getter
@AllArgsConstructor
public enum RoleCodeEnum {

    /**
     * 超级管理员：跳过所有权限校验，仅属于系统租户
     */
    SUPER_ADMIN("super_admin", "超级管理员"),
    /**
     * 普通管理员：初始化数据中的演示角色
     */
    COMMON_ADMIN("common_admin", "普通管理员"),
    /**
     * 租户管理员：新建租户时自动创建，权限为该租户套餐内的全部菜单
     */
    TENANT_ADMIN("tenant_admin", "租户管理员");

    private final String code;
    private final String name;

    public static boolean isSuperAdmin(String code) {
        return Objects.equals(SUPER_ADMIN.code, code);
    }
}
