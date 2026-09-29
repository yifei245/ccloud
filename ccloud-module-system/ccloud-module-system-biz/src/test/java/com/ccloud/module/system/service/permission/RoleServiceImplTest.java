package com.ccloud.module.system.service.permission;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.system.controller.admin.permission.vo.role.RolePageReqVO;
import com.ccloud.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import com.ccloud.module.system.dal.dataobject.permission.RoleDO;
import com.ccloud.module.system.dal.mysql.permission.RoleMapper;
import com.ccloud.module.system.enums.permission.DataScopeEnum;
import com.ccloud.module.system.enums.permission.RoleCodeEnum;
import com.ccloud.module.system.enums.permission.RoleTypeEnum;
import com.ccloud.module.system.service.dept.DeptService;
import com.ccloud.module.system.service.user.AdminUserService;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import com.ccloud.module.system.test.CacheTestConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import({RoleServiceImpl.class, PermissionServiceImpl.class, MenuServiceImpl.class, CacheTestConfiguration.class})
class RoleServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private RoleService roleService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleMapper roleMapper;

    @MockitoBean
    private DeptService deptService;
    @MockitoBean
    private AdminUserService userService;

    // ========== 创建 ==========

    @Test
    void createRole_成功时默认开启且数据范围为全部() {
        Long id = roleService.createRole(newReqVO("运营", "operator"), RoleTypeEnum.CUSTOM.getType());

        RoleDO role = roleMapper.selectById(id);
        assertThat(role.getName()).isEqualTo("运营");
        assertThat(role.getType()).isEqualTo(RoleTypeEnum.CUSTOM.getType());
        assertThat(role.getStatus()).isEqualTo(CommonStatusEnum.ENABLE.getStatus());
        assertThat(role.getDataScope()).isEqualTo(DataScopeEnum.ALL.getScope());
        assertThat(role.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void createRole_名称重复() {
        roleService.createRole(newReqVO("运营", "operator"), null);
        assertServiceException(() -> roleService.createRole(newReqVO("运营", "operator2"), null), ROLE_NAME_DUPLICATE);
    }

    @Test
    void createRole_标识重复() {
        roleService.createRole(newReqVO("运营", "operator"), null);
        assertServiceException(() -> roleService.createRole(newReqVO("运营2", "operator"), null), ROLE_CODE_DUPLICATE);
    }

    @Test
    void createRole_不允许使用超级管理员标识() {
        assertServiceException(() -> roleService.createRole(newReqVO("假冒", "super_admin"), null), ROLE_ADMIN_CODE_ERROR);
    }

    @Test
    void createRole_不同租户可以同名() {
        roleService.createRole(newReqVO("运营", "operator"), null);
        Long otherId = TenantUtils.execute(2L, () -> roleService.createRole(newReqVO("运营", "operator"), null));
        assertThat(otherId).isNotNull();
    }

    // ========== 修改 ==========

    @Test
    void updateRole_成功且清除角色缓存() {
        Long id = roleService.createRole(newReqVO("旧名", "old"), null);
        assertThat(roleService.getRoleFromCache(id).getName()).isEqualTo("旧名");

        RoleSaveReqVO reqVO = newReqVO("新名", "new");
        reqVO.setId(id);
        roleService.updateRole(reqVO);

        assertThat(roleMapper.selectById(id).getCode()).isEqualTo("new");
        assertThat(roleService.getRoleFromCache(id).getName()).isEqualTo("新名");
    }

    @Test
    void updateRole_不存在() {
        RoleSaveReqVO reqVO = newReqVO("x", "x");
        reqVO.setId(-1L);
        assertServiceException(() -> roleService.updateRole(reqVO), ROLE_NOT_EXISTS);
    }

    @Test
    void updateRole_内置角色不允许修改() {
        Long id = roleService.createRole(newReqVO("内置", "builtin"), RoleTypeEnum.SYSTEM.getType());
        RoleSaveReqVO reqVO = newReqVO("改名", "builtin");
        reqVO.setId(id);
        assertServiceException(() -> roleService.updateRole(reqVO), ROLE_CAN_NOT_UPDATE_SYSTEM_TYPE_ROLE);
    }

    @Test
    void updateRole_名称与其他角色重复() {
        roleService.createRole(newReqVO("占用", "taken"), null);
        Long id = roleService.createRole(newReqVO("自己", "self"), null);
        RoleSaveReqVO reqVO = newReqVO("占用", "self");
        reqVO.setId(id);
        assertServiceException(() -> roleService.updateRole(reqVO), ROLE_NAME_DUPLICATE);
    }

    @Test
    void updateRoleStatus_成功且清除缓存() {
        Long id = roleService.createRole(newReqVO("状态", "status"), null);
        roleService.getRoleFromCache(id);
        roleService.updateRoleStatus(id, CommonStatusEnum.DISABLE.getStatus());
        assertThat(roleService.getRoleFromCache(id).getStatus()).isEqualTo(CommonStatusEnum.DISABLE.getStatus());
    }

    @Test
    void updateRoleDataScope_指定部门时保存部门_其他范围清空部门() {
        Long id = roleService.createRole(newReqVO("范围", "scope"), null);

        roleService.updateRoleDataScope(id, DataScopeEnum.DEPT_CUSTOM.getScope(), Set.of(10L, 20L));
        RoleDO role = roleMapper.selectById(id);
        assertThat(role.getDataScope()).isEqualTo(DataScopeEnum.DEPT_CUSTOM.getScope());
        assertThat(role.getDataScopeDeptIds()).containsExactlyInAnyOrder(10L, 20L);

        roleService.updateRoleDataScope(id, DataScopeEnum.SELF.getScope(), Set.of(10L));
        assertThat(roleMapper.selectById(id).getDataScopeDeptIds()).isEmpty();
    }

    // ========== 删除 ==========

    @Test
    void deleteRole_应清理用户与菜单关联() {
        Long id = roleService.createRole(newReqVO("待删", "todelete"), null);
        permissionService.assignRoleMenu(id, Set.of(1L, 2L));
        permissionService.assignUserRole(100L, Set.of(id));

        roleService.deleteRole(id);

        assertThat(roleMapper.selectById(id)).isNull();
        assertThat(permissionService.getRoleMenuListByRoleId(id)).isEmpty();
        assertThat(permissionService.getUserRoleIdListByUserId(100L)).isEmpty();
        assertThat(roleService.getRoleFromCache(id)).isNull();
    }

    @Test
    void deleteRole_内置角色不允许删除() {
        Long id = roleService.createRole(newReqVO("内置", "builtin"), RoleTypeEnum.SYSTEM.getType());
        assertServiceException(() -> roleService.deleteRole(id), ROLE_CAN_NOT_UPDATE_SYSTEM_TYPE_ROLE);
    }

    @Test
    void deleteRoleList() {
        Long a = roleService.createRole(newReqVO("甲", "a"), null);
        Long b = roleService.createRole(newReqVO("乙", "b"), null);
        roleService.deleteRoleList(List.of(a, b));
        assertThat(roleMapper.selectCount()).isZero();
    }

    // ========== 查询与校验 ==========

    @Test
    void getRolePage_按条件过滤() {
        roleService.createRole(newReqVO("财务主管", "finance_leader"), null);
        Long disabled = roleService.createRole(newReqVO("财务助理", "finance_assistant"), null);
        roleService.updateRoleStatus(disabled, CommonStatusEnum.DISABLE.getStatus());
        roleService.createRole(newReqVO("销售", "sales"), null);

        RolePageReqVO reqVO = new RolePageReqVO();
        reqVO.setName("财务");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        PageResult<RoleDO> page = roleService.getRolePage(reqVO);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getList()).extracting(RoleDO::getCode).containsExactly("finance_leader");

        reqVO = new RolePageReqVO();
        reqVO.setCode("finance");
        assertThat(roleService.getRolePage(reqVO).getTotal()).isEqualTo(2);
    }

    @Test
    void getRole与列表查询只返回当前租户数据() {
        Long mine = roleService.createRole(newReqVO("本租户", "mine"), null);
        TenantUtils.execute(2L, () -> roleService.createRole(newReqVO("他租户", "other"), null));

        assertThat(roleService.getRole(mine).getName()).isEqualTo("本租户");
        assertThat(roleService.getRoleList()).extracting(RoleDO::getName).containsExactly("本租户");
        assertThat(roleService.getRoleList(List.of(mine))).hasSize(1);
        assertThat(roleService.getRoleListByStatus(Set.of(CommonStatusEnum.ENABLE.getStatus()))).hasSize(1);
        assertThat(roleService.getRoleListFromCache(List.of(mine, -1L))).extracting(RoleDO::getId).containsExactly(mine);
    }

    @Test
    void hasAnySuperAdmin() {
        Long normal = roleService.createRole(newReqVO("普通", "normal"), null);
        Long superAdmin = insertSuperAdminRole();

        assertThat(roleService.hasAnySuperAdmin(List.of(normal))).isFalse();
        assertThat(roleService.hasAnySuperAdmin(List.of(normal, superAdmin))).isTrue();
        assertThat(roleService.hasAnySuperAdmin(List.of())).isFalse();
    }

    @Test
    void validateRoleList_不存在与已禁用() {
        Long ok = roleService.createRole(newReqVO("可用", "ok"), null);
        Long disabled = roleService.createRole(newReqVO("禁用", "off"), null);
        roleService.updateRoleStatus(disabled, CommonStatusEnum.DISABLE.getStatus());

        roleService.validateRoleList(List.of(ok));
        assertServiceException(() -> roleService.validateRoleList(List.of(-1L)), ROLE_NOT_EXISTS);
        assertServiceException(() -> roleService.validateRoleList(List.of(disabled)), ROLE_IS_DISABLE);
    }

    private Long insertSuperAdminRole() {
        RoleDO role = new RoleDO().setName("超级管理员").setCode(RoleCodeEnum.SUPER_ADMIN.getCode()).setSort(0)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setType(RoleTypeEnum.SYSTEM.getType())
                .setDataScope(DataScopeEnum.ALL.getScope());
        roleMapper.insert(role);
        return role.getId();
    }

    private static RoleSaveReqVO newReqVO(String name, String code) {
        RoleSaveReqVO reqVO = new RoleSaveReqVO();
        reqVO.setName(name);
        reqVO.setCode(code);
        reqVO.setSort(1);
        reqVO.setRemark("");
        return reqVO;
    }
}
