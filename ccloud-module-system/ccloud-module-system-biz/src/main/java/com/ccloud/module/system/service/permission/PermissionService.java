package com.ccloud.module.system.service.permission;

import java.util.Collection;
import java.util.Set;

/**
 * 权限 Service：角色-菜单、用户-角色的授权关系
 */
public interface PermissionService {

    /**
     * 设置角色拥有的菜单，按差量增删
     */
    void assignRoleMenu(Long roleId, Collection<Long> menuIds);

    Set<Long> getRoleMenuListByRoleId(Long roleId);

    /**
     * 设置用户拥有的角色，按差量增删
     */
    void assignUserRole(Long userId, Collection<Long> roleIds);

    Set<Long> getUserRoleIdListByUserId(Long userId);

    /**
     * 用户删除后清理其角色关联
     */
    void processUserDeleted(Long userId);
}
