package com.ccloud.framework.security.core.service;

/**
 * 权限判定契约。实现由 system 模块经 RPC 提供。
 */
public interface PermissionFrameworkService {

    /**
     * @return 用户是否拥有其中任意一个权限；超级管理员恒为 true
     */
    boolean hasAnyPermissions(Long userId, String... permissions);

    /**
     * @return 用户是否拥有其中任意一个角色
     */
    boolean hasAnyRoles(Long userId, String... roles);
}
