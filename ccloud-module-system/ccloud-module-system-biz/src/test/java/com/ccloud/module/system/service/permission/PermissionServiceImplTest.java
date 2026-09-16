package com.ccloud.module.system.service.permission;

import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Import(PermissionServiceImpl.class)
class PermissionServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private PermissionServiceImpl permissionService;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    void assignRoleMenu_按差量增删且保留未变化的记录() {
        permissionService.assignRoleMenu(10L, Set.of(1L, 2L, 3L));
        Long keptRowId = jdbcTemplate.queryForObject(
                "SELECT id FROM system_role_menu WHERE role_id = 10 AND menu_id = 2 AND deleted = 0", Long.class);

        permissionService.assignRoleMenu(10L, Set.of(2L, 4L));

        assertThat(permissionService.getRoleMenuListByRoleId(10L)).containsExactlyInAnyOrder(2L, 4L);
        // 未变化的菜单不应被删除重建
        assertThat(jdbcTemplate.queryForObject(
                "SELECT id FROM system_role_menu WHERE role_id = 10 AND menu_id = 2 AND deleted = 0", Long.class))
                .isEqualTo(keptRowId);
    }

    @Test
    void assignRoleMenu_传空集合时清空() {
        permissionService.assignRoleMenu(11L, Set.of(1L));
        permissionService.assignRoleMenu(11L, Collections.emptySet());
        assertThat(permissionService.getRoleMenuListByRoleId(11L)).isEmpty();
    }

    @Test
    void assignRoleMenu_不影响其他角色() {
        permissionService.assignRoleMenu(12L, Set.of(1L));
        permissionService.assignRoleMenu(13L, Set.of(1L, 2L));
        permissionService.assignRoleMenu(12L, Collections.emptySet());
        assertThat(permissionService.getRoleMenuListByRoleId(13L)).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void assignUserRole_按差量增删() {
        permissionService.assignUserRole(20L, Set.of(1L, 2L));
        permissionService.assignUserRole(20L, Set.of(2L, 3L));
        assertThat(permissionService.getUserRoleIdListByUserId(20L)).containsExactlyInAnyOrder(2L, 3L);
    }
}
