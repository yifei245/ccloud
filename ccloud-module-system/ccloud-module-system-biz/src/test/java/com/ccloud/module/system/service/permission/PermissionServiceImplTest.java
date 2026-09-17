package com.ccloud.module.system.service.permission;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionRespDTO;
import com.ccloud.framework.security.core.service.PermissionCacheRefresher;
import com.ccloud.module.system.dal.dataobject.dept.DeptDO;
import com.ccloud.module.system.dal.dataobject.permission.MenuDO;
import com.ccloud.module.system.dal.dataobject.permission.RoleDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.dal.mysql.permission.MenuMapper;
import com.ccloud.module.system.dal.mysql.permission.RoleMapper;
import com.ccloud.module.system.enums.permission.DataScopeEnum;
import com.ccloud.module.system.enums.permission.MenuTypeEnum;
import com.ccloud.module.system.enums.permission.RoleCodeEnum;
import com.ccloud.module.system.enums.permission.RoleTypeEnum;
import com.ccloud.module.system.service.dept.DeptService;
import com.ccloud.module.system.service.user.AdminUserService;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import com.ccloud.module.system.test.CacheTestConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.ccloud.module.system.enums.ErrorCodeConstants.ROLE_SUPER_ADMIN_ASSIGN_DENIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@Import({PermissionServiceImpl.class, RoleServiceImpl.class, MenuServiceImpl.class, CacheTestConfiguration.class})
class PermissionServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private MenuMapper menuMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private DeptService deptService;
    @MockitoBean
    private AdminUserService userService;
    @MockitoBean
    private PermissionCacheRefresher permissionCacheRefresher;

    // ========== 授权关系 ==========

    @Test
    void assignRoleMenu_按差量增删且保留未变化的记录() {
        permissionService.assignRoleMenu(10L, Set.of(1L, 2L, 3L));
        Long keptRowId = jdbcTemplate.queryForObject(
                "SELECT id FROM system_role_menu WHERE role_id = 10 AND menu_id = 2 AND deleted = 0", Long.class);

        permissionService.assignRoleMenu(10L, Set.of(2L, 4L));

        assertThat(permissionService.getRoleMenuListByRoleId(10L)).containsExactlyInAnyOrder(2L, 4L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT id FROM system_role_menu WHERE role_id = 10 AND menu_id = 2 AND deleted = 0", Long.class))
                .isEqualTo(keptRowId);
    }

    @Test
    void assignRoleMenu_传空集合时清空_且不影响其他角色() {
        permissionService.assignRoleMenu(11L, Set.of(1L));
        permissionService.assignRoleMenu(12L, Set.of(1L, 2L));
        permissionService.assignRoleMenu(11L, Collections.emptySet());
        assertThat(permissionService.getRoleMenuListByRoleId(11L)).isEmpty();
        assertThat(permissionService.getRoleMenuListByRoleId(12L)).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void assignUserRole_按差量增删() {
        permissionService.assignUserRole(20L, Set.of(1L, 2L));
        permissionService.assignUserRole(20L, Set.of(2L, 3L));
        assertThat(permissionService.getUserRoleIdListByUserId(20L)).containsExactlyInAnyOrder(2L, 3L);
        assertThat(permissionService.getUserRoleIdListByRoleIds(List.of(3L))).containsExactly(20L);
    }

    @Test
    void getRoleMenuListByRoleIds_超级管理员拥有全部菜单() {
        Long superAdmin = insertRole("super_admin", CommonStatusEnum.ENABLE, DataScopeEnum.ALL, null);
        MenuDO m1 = insertMenu("a:b:c", CommonStatusEnum.ENABLE);
        MenuDO m2 = insertMenu("d:e:f", CommonStatusEnum.ENABLE);
        assertThat(permissionService.getRoleMenuListByRoleId(List.of(superAdmin)))
                .containsExactlyInAnyOrder(m1.getId(), m2.getId());
    }

    // ========== 缓存失效 ==========

    @Test
    void assignUserRole后缓存的用户角色应失效() {
        permissionService.assignUserRole(30L, Set.of(1L));
        assertThat(permissionService.getUserRoleIdListByUserIdFromCache(30L)).containsExactly(1L);

        permissionService.assignUserRole(30L, Set.of(2L));

        assertThat(permissionService.getUserRoleIdListByUserIdFromCache(30L)).containsExactly(2L);
    }

    @Test
    void assignRoleMenu后缓存的菜单角色应失效_权限判断立即生效() {
        Long roleId = insertRole("editor", CommonStatusEnum.ENABLE, DataScopeEnum.ALL, null);
        MenuDO menu = insertMenu("system:user:update", CommonStatusEnum.ENABLE);
        permissionService.assignUserRole(40L, Set.of(roleId));
        assertThat(permissionService.hasAnyPermissions(40L, "system:user:update")).isFalse();

        permissionService.assignRoleMenu(roleId, Set.of(menu.getId()));

        assertThat(permissionService.hasAnyPermissions(40L, "system:user:update")).isTrue();
    }

    @Test
    void 授权变更时向各节点广播刷新() {
        permissionService.assignUserRole(50L, Set.of(1L));
        verify(permissionCacheRefresher).publish(50L);
        permissionService.assignRoleMenu(1L, Set.of(1L));
        verify(permissionCacheRefresher).publish(null);
    }

    // ========== 权限判断 ==========

    @Test
    void hasAnyPermissions_超级管理员对任意权限返回true() {
        Long superAdmin = insertRole(RoleCodeEnum.SUPER_ADMIN.getCode(), CommonStatusEnum.ENABLE, DataScopeEnum.ALL, null);
        permissionService.assignUserRole(1L, Set.of(superAdmin));
        assertThat(permissionService.hasAnyPermissions(1L, "not:defined:anywhere")).isTrue();
        assertThat(permissionService.hasAnyRoles(1L, "whatever")).isTrue();
    }

    @Test
    void hasAnyPermissions_多角色权限取并集() {
        MenuDO create = insertMenu("system:user:create", CommonStatusEnum.ENABLE);
        MenuDO delete = insertMenu("system:user:delete", CommonStatusEnum.ENABLE);
        Long r1 = insertRole("r1", CommonStatusEnum.ENABLE, DataScopeEnum.ALL, null);
        Long r2 = insertRole("r2", CommonStatusEnum.ENABLE, DataScopeEnum.ALL, null);
        permissionService.assignRoleMenu(r1, Set.of(create.getId()));
        permissionService.assignRoleMenu(r2, Set.of(delete.getId()));
        permissionService.assignUserRole(2L, Set.of(r1, r2));

        assertThat(permissionService.hasAnyPermissions(2L, "system:user:create")).isTrue();
        assertThat(permissionService.hasAnyPermissions(2L, "system:user:delete")).isTrue();
        assertThat(permissionService.hasAnyPermissions(2L, "system:user:export")).isFalse();
        assertThat(permissionService.hasAnyPermissions(2L, "system:user:export", "system:user:delete")).isTrue();
    }

    @Test
    void hasAnyPermissions_禁用的角色与禁用的菜单不计入() {
        MenuDO enabledMenu = insertMenu("p:enabled", CommonStatusEnum.ENABLE);
        MenuDO disabledMenu = insertMenu("p:disabled-menu", CommonStatusEnum.DISABLE);
        MenuDO viaDisabledRole = insertMenu("p:disabled-role", CommonStatusEnum.ENABLE);
        Long enabledRole = insertRole("on", CommonStatusEnum.ENABLE, DataScopeEnum.ALL, null);
        Long disabledRole = insertRole("off", CommonStatusEnum.DISABLE, DataScopeEnum.ALL, null);
        permissionService.assignRoleMenu(enabledRole, Set.of(enabledMenu.getId(), disabledMenu.getId()));
        permissionService.assignRoleMenu(disabledRole, Set.of(viaDisabledRole.getId()));
        permissionService.assignUserRole(3L, Set.of(enabledRole, disabledRole));

        assertThat(permissionService.hasAnyPermissions(3L, "p:enabled")).isTrue();
        assertThat(permissionService.hasAnyPermissions(3L, "p:disabled-menu")).isFalse();
        assertThat(permissionService.hasAnyPermissions(3L, "p:disabled-role")).isFalse();
    }

    @Test
    void hasAnyPermissions_没有角色时返回false_空权限列表返回true() {
        assertThat(permissionService.hasAnyPermissions(4L, "system:user:query")).isFalse();
        assertThat(permissionService.hasAnyPermissions(4L)).isTrue();
    }

    @Test
    void hasAnyRoles_按角色标识匹配且忽略禁用角色() {
        Long on = insertRole("auditor", CommonStatusEnum.ENABLE, DataScopeEnum.ALL, null);
        Long off = insertRole("finance", CommonStatusEnum.DISABLE, DataScopeEnum.ALL, null);
        permissionService.assignUserRole(5L, Set.of(on, off));

        assertThat(permissionService.hasAnyRoles(5L, "auditor")).isTrue();
        assertThat(permissionService.hasAnyRoles(5L, "finance")).isFalse();
        assertThat(permissionService.hasAnyRoles(5L, "finance", "auditor")).isTrue();
    }

    // ========== 数据权限 ==========

    @Test
    void getDeptDataPermission_全部() {
        assignRolesTo(60L, insertRole("all", CommonStatusEnum.ENABLE, DataScopeEnum.ALL, null));
        DeptDataPermissionRespDTO dto = permissionService.getDeptDataPermission(60L);
        assertThat(dto.isAll()).isTrue();
    }

    @Test
    void getDeptDataPermission_指定部门_同时包含本部门() {
        mockUserDept(61L, 100L);
        assignRolesTo(61L, insertRole("custom", CommonStatusEnum.ENABLE, DataScopeEnum.DEPT_CUSTOM, Set.of(7L, 8L)));
        DeptDataPermissionRespDTO dto = permissionService.getDeptDataPermission(61L);
        assertThat(dto.isAll()).isFalse();
        assertThat(dto.isSelf()).isFalse();
        assertThat(dto.getDeptIds()).containsExactlyInAnyOrder(7L, 8L, 100L);
    }

    @Test
    void getDeptDataPermission_本部门() {
        mockUserDept(62L, 100L);
        assignRolesTo(62L, insertRole("dept", CommonStatusEnum.ENABLE, DataScopeEnum.DEPT_ONLY, null));
        assertThat(permissionService.getDeptDataPermission(62L).getDeptIds()).containsExactly(100L);
    }

    @Test
    void getDeptDataPermission_本部门及以下() {
        mockUserDept(63L, 100L);
        when(deptService.getChildDeptList(100L)).thenReturn(List.of(
                new DeptDO().setId(101L), new DeptDO().setId(102L)));
        assignRolesTo(63L, insertRole("tree", CommonStatusEnum.ENABLE, DataScopeEnum.DEPT_AND_CHILD, null));
        assertThat(permissionService.getDeptDataPermission(63L).getDeptIds()).containsExactlyInAnyOrder(100L, 101L, 102L);
    }

    @Test
    void getDeptDataPermission_仅本人() {
        assignRolesTo(64L, insertRole("self", CommonStatusEnum.ENABLE, DataScopeEnum.SELF, null));
        DeptDataPermissionRespDTO dto = permissionService.getDeptDataPermission(64L);
        assertThat(dto.isSelf()).isTrue();
        assertThat(dto.getDeptIds()).isEmpty();
    }

    @Test
    void getDeptDataPermission_多角色取并集_全部优先() {
        mockUserDept(65L, 100L);
        Long custom = insertRole("c", CommonStatusEnum.ENABLE, DataScopeEnum.DEPT_CUSTOM, Set.of(7L));
        Long self = insertRole("s", CommonStatusEnum.ENABLE, DataScopeEnum.SELF, null);
        assignRolesTo(65L, custom, self);
        DeptDataPermissionRespDTO dto = permissionService.getDeptDataPermission(65L);
        assertThat(dto.getDeptIds()).containsExactlyInAnyOrder(7L, 100L);
        assertThat(dto.isSelf()).isTrue();

        Long all = insertRole("a", CommonStatusEnum.ENABLE, DataScopeEnum.ALL, null);
        assignRolesTo(65L, custom, self, all);
        assertThat(permissionService.getDeptDataPermission(65L).isAll()).isTrue();
    }

    @Test
    void getDeptDataPermission_没有启用的角色时仅本人() {
        Long disabled = insertRole("d", CommonStatusEnum.DISABLE, DataScopeEnum.ALL, null);
        assignRolesTo(66L, disabled);
        DeptDataPermissionRespDTO dto = permissionService.getDeptDataPermission(66L);
        assertThat(dto.isAll()).isFalse();
        assertThat(dto.isSelf()).isTrue();
        assertThat(dto.getDeptIds()).isEmpty();
    }

    @Test
    void getDeptDataPermission_用户没有部门时本部门范围为空() {
        when(userService.getUser(67L)).thenReturn(new AdminUserDO().setId(67L));
        assignRolesTo(67L, insertRole("nodept", CommonStatusEnum.ENABLE, DataScopeEnum.DEPT_AND_CHILD, null));
        assertThat(permissionService.getDeptDataPermission(67L).getDeptIds()).isEmpty();
        verify(deptService, never()).getChildDeptList(anyLong());
    }

    // ========== 清理 ==========

    @Test
    void processRoleDeleted_清理角色的用户与菜单关联() {
        permissionService.assignRoleMenu(70L, Set.of(1L));
        permissionService.assignUserRole(71L, Set.of(70L, 72L));
        permissionService.processRoleDeleted(70L);
        assertThat(permissionService.getRoleMenuListByRoleId(70L)).isEmpty();
        assertThat(permissionService.getUserRoleIdListByUserId(71L)).containsExactly(72L);
    }

    @Test
    void processMenuDeleted_清理菜单的角色关联() {
        permissionService.assignRoleMenu(80L, Set.of(5L, 6L));
        permissionService.processMenuDeleted(5L);
        assertThat(permissionService.getRoleMenuListByRoleId(80L)).containsExactly(6L);
    }

    @Test
    void processUserDeleted_清理用户角色关联() {
        permissionService.assignUserRole(90L, Set.of(1L));
        permissionService.processUserDeleted(90L);
        assertThat(permissionService.getUserRoleIdListByUserId(90L)).isEmpty();
    }

    // ========== 分配超管角色的越权防护 ==========

    @Test
    void validateAssignableRoles_非超管不能分配超管角色() {
        Long superAdmin = insertRole(RoleCodeEnum.SUPER_ADMIN.getCode(), CommonStatusEnum.ENABLE, DataScopeEnum.ALL, null);
        Long normal = insertRole("normal", CommonStatusEnum.ENABLE, DataScopeEnum.ALL, null);
        permissionService.assignUserRole(200L, Set.of(normal));

        assertServiceException(() -> permissionService.validateAssignableRoles(200L, Set.of(normal, superAdmin)),
                ROLE_SUPER_ADMIN_ASSIGN_DENIED);
        permissionService.validateAssignableRoles(200L, Set.of(normal));

        permissionService.assignUserRole(201L, Set.of(superAdmin));
        permissionService.validateAssignableRoles(201L, Set.of(superAdmin));
    }

    // ========== 工具 ==========

    private void assignRolesTo(Long userId, Long... roleIds) {
        permissionService.assignUserRole(userId, Set.of(roleIds));
    }

    private void mockUserDept(Long userId, Long deptId) {
        when(userService.getUser(userId)).thenReturn(new AdminUserDO().setId(userId).setDeptId(deptId));
    }

    private Long insertRole(String code, CommonStatusEnum status, DataScopeEnum dataScope, Set<Long> deptIds) {
        RoleDO role = new RoleDO().setName(code).setCode(code).setSort(0).setStatus(status.getStatus())
                .setType(RoleTypeEnum.CUSTOM.getType()).setDataScope(dataScope.getScope()).setDataScopeDeptIds(deptIds);
        roleMapper.insert(role);
        return role.getId();
    }

    private MenuDO insertMenu(String permission, CommonStatusEnum status) {
        MenuDO menu = new MenuDO().setName(permission).setPermission(permission).setType(MenuTypeEnum.BUTTON.getType())
                .setSort(0).setParentId(0L).setStatus(status.getStatus());
        menuMapper.insert(menu);
        return menu;
    }
}
