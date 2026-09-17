package com.ccloud.module.system.service.permission;

import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionRespDTO;

import java.util.Collection;
import java.util.Set;

/**
 * 权限 Service：角色-菜单、用户-角色的授权关系，以及基于它们的权限判断与数据范围计算
 */
public interface PermissionService {

    // ========== 权限判断 ==========

    /**
     * 超级管理员直接通过；否则任一权限标识被用户任一开启的角色通过开启的菜单持有即通过
     */
    boolean hasAnyPermissions(Long userId, String... permissions);

    /**
     * 超级管理员直接通过；否则用户任一开启的角色标识命中即通过
     */
    boolean hasAnyRoles(Long userId, String... roles);

    // ========== 角色-菜单 ==========

    /**
     * 设置角色拥有的菜单，按差量增删
     */
    void assignRoleMenu(Long roleId, Collection<Long> menuIds);

    Set<Long> getRoleMenuListByRoleId(Long roleId);

    /**
     * 含超级管理员角色时返回全部菜单
     */
    Set<Long> getRoleMenuListByRoleId(Collection<Long> roleIds);

    Set<Long> getMenuRoleIdListByMenuIdFromCache(Long menuId);

    // ========== 用户-角色 ==========

    /**
     * 设置用户拥有的角色，按差量增删
     */
    void assignUserRole(Long userId, Collection<Long> roleIds);

    Set<Long> getUserRoleIdListByUserId(Long userId);

    Set<Long> getUserRoleIdListByUserIdFromCache(Long userId);

    Set<Long> getUserRoleIdListByRoleIds(Collection<Long> roleIds);

    /**
     * 校验操作人可以分配这些角色：只有超级管理员才能分配超级管理员角色，防止越权自提权
     */
    void validateAssignableRoles(Long operatorUserId, Collection<Long> roleIds);

    // ========== 数据权限 ==========

    /**
     * 计算用户的部门数据范围：各开启角色的范围取并集，任一角色为"全部"即全部；没有开启的角色时仅本人
     */
    DeptDataPermissionRespDTO getDeptDataPermission(Long userId);

    // ========== 关联清理 ==========

    void processRoleDeleted(Long roleId);

    void processMenuDeleted(Long menuId);

    void processUserDeleted(Long userId);
}
