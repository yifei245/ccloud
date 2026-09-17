package com.ccloud.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionRespDTO;
import com.ccloud.framework.security.core.service.PermissionCacheRefresher;
import com.ccloud.module.system.dal.dataobject.permission.MenuDO;
import com.ccloud.module.system.dal.dataobject.permission.RoleDO;
import com.ccloud.module.system.dal.dataobject.permission.RoleMenuDO;
import com.ccloud.module.system.dal.dataobject.permission.UserRoleDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.dal.mysql.permission.RoleMenuMapper;
import com.ccloud.module.system.dal.mysql.permission.UserRoleMapper;
import com.ccloud.module.system.enums.permission.DataScopeEnum;
import com.ccloud.module.system.service.dept.DeptService;
import com.ccloud.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Supplier;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.dal.redis.RedisKeyConstants.MENU_ROLE_ID_LIST;
import static com.ccloud.module.system.dal.redis.RedisKeyConstants.USER_ROLE_ID_LIST;
import static com.ccloud.module.system.enums.ErrorCodeConstants.ROLE_SUPER_ADMIN_ASSIGN_DENIED;

@Service
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    @Lazy
    private RoleService roleService;
    @Resource
    @Lazy
    private MenuService menuService;
    @Resource
    @Lazy
    private DeptService deptService;
    @Resource
    @Lazy
    private AdminUserService userService;
    /**
     * 自身代理：带缓存注解的方法在类内调用时必须经过代理才生效
     */
    @Resource
    @Lazy
    private PermissionService self;
    @Resource
    private ObjectProvider<PermissionCacheRefresher> permissionCacheRefresherProvider;

    // ========== 权限判断 ==========

    @Override
    public boolean hasAnyPermissions(Long userId, String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }
        List<RoleDO> roles = getEnableUserRoleList(userId);
        if (roles.isEmpty()) {
            return false;
        }
        Set<Long> roleIds = CollectionUtils.convertSet(roles, RoleDO::getId);
        if (roleService.hasAnySuperAdmin(roleIds)) {
            return true;
        }
        return Arrays.stream(permissions).anyMatch(permission -> hasPermission(roleIds, permission));
    }

    /**
     * 权限标识 → 开启的菜单 → 持有该菜单的角色，与用户的开启角色有交集即通过
     */
    private boolean hasPermission(Set<Long> roleIds, String permission) {
        List<Long> menuIds = menuService.getMenuIdListByPermissionFromCache(permission);
        for (Long menuId : menuIds) {
            if (CollUtil.containsAny(self.getMenuRoleIdListByMenuIdFromCache(menuId), roleIds)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnyRoles(Long userId, String... roles) {
        if (roles == null || roles.length == 0) {
            return true;
        }
        List<RoleDO> userRoles = getEnableUserRoleList(userId);
        if (userRoles.isEmpty()) {
            return false;
        }
        if (roleService.hasAnySuperAdmin(CollectionUtils.convertSet(userRoles, RoleDO::getId))) {
            return true;
        }
        Set<String> codes = CollectionUtils.convertSet(userRoles, RoleDO::getCode);
        return Arrays.stream(roles).anyMatch(codes::contains);
    }

    private List<RoleDO> getEnableUserRoleList(Long userId) {
        Set<Long> roleIds = self.getUserRoleIdListByUserIdFromCache(userId);
        return CollectionUtils.filterList(roleService.getRoleListFromCache(roleIds),
                role -> CommonStatusEnum.isEnable(role.getStatus()));
    }

    // ========== 角色-菜单 ==========

    /**
     * 差量更新而非"全删再插"：未变化的记录保持原样，审计字段与逻辑删除历史不会被无谓刷新。
     * 菜单-角色缓存按菜单为键，无法精确定位受影响的键，整体清除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = MENU_ROLE_ID_LIST, allEntries = true)
    public void assignRoleMenu(Long roleId, Collection<Long> menuIds) {
        Set<Long> dbMenuIds = getRoleMenuListByRoleId(roleId);
        Set<Long> targetMenuIds = menuIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(menuIds);

        Collection<Long> createMenuIds = CollUtil.subtract(targetMenuIds, dbMenuIds);
        Collection<Long> deleteMenuIds = CollUtil.subtract(dbMenuIds, targetMenuIds);
        if (CollUtil.isNotEmpty(createMenuIds)) {
            roleMenuMapper.insertBatch(CollectionUtils.convertList(createMenuIds,
                    menuId -> new RoleMenuDO().setRoleId(roleId).setMenuId(menuId)));
        }
        if (CollUtil.isNotEmpty(deleteMenuIds)) {
            roleMenuMapper.deleteListByRoleIdAndMenuIds(roleId, deleteMenuIds);
        }
        refreshPermissionCache(null);
    }

    @Override
    public Set<Long> getRoleMenuListByRoleId(Long roleId) {
        return CollectionUtils.convertSet(roleMenuMapper.selectListByRoleId(roleId), RoleMenuDO::getMenuId);
    }

    @Override
    public Set<Long> getRoleMenuListByRoleId(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptySet();
        }
        if (roleService.hasAnySuperAdmin(roleIds)) {
            return CollectionUtils.convertSet(menuService.getMenuList(), MenuDO::getId);
        }
        return CollectionUtils.convertSet(roleMenuMapper.selectListByRoleId(roleIds), RoleMenuDO::getMenuId);
    }

    @Override
    @Cacheable(value = MENU_ROLE_ID_LIST, key = "#menuId")
    public Set<Long> getMenuRoleIdListByMenuIdFromCache(Long menuId) {
        return CollectionUtils.convertSet(roleMenuMapper.selectListByMenuId(menuId), RoleMenuDO::getRoleId);
    }

    // ========== 用户-角色 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = USER_ROLE_ID_LIST, key = "#userId")
    public void assignUserRole(Long userId, Collection<Long> roleIds) {
        Set<Long> dbRoleIds = getUserRoleIdListByUserId(userId);
        Set<Long> targetRoleIds = roleIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(roleIds);

        Collection<Long> createRoleIds = CollUtil.subtract(targetRoleIds, dbRoleIds);
        Collection<Long> deleteRoleIds = CollUtil.subtract(dbRoleIds, targetRoleIds);
        if (CollUtil.isNotEmpty(createRoleIds)) {
            userRoleMapper.insertBatch(CollectionUtils.convertList(createRoleIds,
                    roleId -> new UserRoleDO().setUserId(userId).setRoleId(roleId)));
        }
        if (CollUtil.isNotEmpty(deleteRoleIds)) {
            userRoleMapper.deleteListByUserIdAndRoleIds(userId, deleteRoleIds);
        }
        refreshPermissionCache(userId);
    }

    @Override
    public Set<Long> getUserRoleIdListByUserId(Long userId) {
        return CollectionUtils.convertSet(userRoleMapper.selectListByUserId(userId), UserRoleDO::getRoleId);
    }

    @Override
    @Cacheable(value = USER_ROLE_ID_LIST, key = "#userId")
    public Set<Long> getUserRoleIdListByUserIdFromCache(Long userId) {
        return getUserRoleIdListByUserId(userId);
    }

    @Override
    public Set<Long> getUserRoleIdListByRoleIds(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptySet();
        }
        return CollectionUtils.convertSet(userRoleMapper.selectListByRoleIds(roleIds), UserRoleDO::getUserId);
    }

    @Override
    public void validateAssignableRoles(Long operatorUserId, Collection<Long> roleIds) {
        if (!roleService.hasAnySuperAdmin(roleIds)) {
            return;
        }
        if (!roleService.hasAnySuperAdmin(getUserRoleIdListByUserId(operatorUserId))) {
            throw exception(ROLE_SUPER_ADMIN_ASSIGN_DENIED);
        }
    }

    // ========== 数据权限 ==========

    @Override
    public DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
        DeptDataPermissionRespDTO result = new DeptDataPermissionRespDTO();
        List<RoleDO> roles = getEnableUserRoleList(userId);
        if (roles.isEmpty()) {
            return result.setSelf(true);
        }

        Set<Long> deptIds = new HashSet<>();
        // 用户所在部门按需查询且只查一次
        Supplier<Long> userDeptId = memoize(() -> {
            AdminUserDO user = userService.getUser(userId);
            return user != null ? user.getDeptId() : null;
        });
        for (RoleDO role : roles) {
            DataScopeEnum scope = DataScopeEnum.valueOf(role.getDataScope());
            if (scope == null) {
                continue;
            }
            switch (scope) {
                case ALL -> {
                    return result.setAll(true);
                }
                case DEPT_CUSTOM -> {
                    CollUtil.addAll(deptIds, role.getDataScopeDeptIds());
                    // 指定部门时同时可见本部门，避免连自己部门的数据都看不到
                    CollectionUtils.addIfNotNull(deptIds, userDeptId.get());
                }
                case DEPT_ONLY -> CollectionUtils.addIfNotNull(deptIds, userDeptId.get());
                case DEPT_AND_CHILD -> {
                    Long deptId = userDeptId.get();
                    if (deptId != null) {
                        deptIds.add(deptId);
                        deptIds.addAll(deptService.getChildDeptIdListFromCache(deptId));
                    }
                }
                case SELF -> result.setSelf(true);
            }
        }
        return result.setDeptIds(deptIds);
    }

    private static <T> Supplier<T> memoize(Supplier<T> supplier) {
        return new Supplier<>() {
            private boolean loaded;
            private T value;

            @Override
            public T get() {
                if (!loaded) {
                    value = supplier.get();
                    loaded = true;
                }
                return value;
            }
        };
    }

    // ========== 关联清理 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = MENU_ROLE_ID_LIST, allEntries = true),
            @CacheEvict(value = USER_ROLE_ID_LIST, allEntries = true)
    })
    public void processRoleDeleted(Long roleId) {
        userRoleMapper.deleteListByRoleId(roleId);
        roleMenuMapper.deleteListByRoleId(roleId);
        refreshPermissionCache(null);
    }

    @Override
    @CacheEvict(value = MENU_ROLE_ID_LIST, key = "#menuId")
    public void processMenuDeleted(Long menuId) {
        roleMenuMapper.deleteListByMenuId(menuId);
        refreshPermissionCache(null);
    }

    @Override
    @CacheEvict(value = USER_ROLE_ID_LIST, key = "#userId")
    public void processUserDeleted(Long userId) {
        userRoleMapper.deleteListByUserId(userId);
        refreshPermissionCache(userId);
    }

    /**
     * 通知各节点清理 {@code @ss} 的本地权限缓存；未接入 Redis 的环境（如单测）跳过
     */
    private void refreshPermissionCache(Long userId) {
        permissionCacheRefresherProvider.ifAvailable(refresher -> refresher.publish(userId));
    }
}
