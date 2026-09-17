package com.ccloud.module.system.service.tenant;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.system.controller.admin.tenant.vo.tenant.TenantPageReqVO;
import com.ccloud.module.system.controller.admin.tenant.vo.tenant.TenantSaveReqVO;
import com.ccloud.module.system.dal.dataobject.permission.RoleDO;
import com.ccloud.module.system.dal.dataobject.tenant.TenantDO;
import com.ccloud.module.system.dal.dataobject.tenant.TenantPackageDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.dal.mysql.permission.RoleMapper;
import com.ccloud.module.system.dal.mysql.tenant.TenantMapper;
import com.ccloud.module.system.dal.mysql.tenant.TenantPackageMapper;
import com.ccloud.module.system.dal.mysql.user.AdminUserMapper;
import com.ccloud.module.system.enums.permission.RoleCodeEnum;
import com.ccloud.module.system.enums.permission.RoleTypeEnum;
import com.ccloud.module.system.service.permission.PermissionService;
import com.ccloud.module.system.service.permission.PermissionServiceImpl;
import com.ccloud.module.system.service.permission.RoleServiceImpl;
import com.ccloud.module.system.service.user.AdminUserServiceImpl;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import({TenantServiceImpl.class, TenantPackageServiceImpl.class, RoleServiceImpl.class,
        PermissionServiceImpl.class, AdminUserServiceImpl.class,
        com.ccloud.module.system.service.dept.DeptServiceImpl.class, com.ccloud.module.system.service.dept.PostServiceImpl.class,
        org.springframework.validation.beanvalidation.LocalValidatorFactoryBean.class})
class TenantServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private TenantServiceImpl tenantService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private TenantMapper tenantMapper;
    @Resource
    private TenantPackageMapper tenantPackageMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private JdbcTemplate jdbcTemplate;

    // ========== 校验 ==========

    @Test
    void validTenant_不存在() {
        assertServiceException(() -> tenantService.validTenant(-1L), TENANT_NOT_EXISTS);
    }

    @Test
    void validTenant_已禁用() {
        TenantDO tenant = insertTenant("禁用租户", null, 99L, t -> t.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        assertServiceException(() -> tenantService.validTenant(tenant.getId()), TENANT_DISABLE);
    }

    @Test
    void validTenant_已过期() {
        TenantDO tenant = insertTenant("过期租户", null, 99L, t -> t.setExpireTime(LocalDateTime.now().minusMinutes(1)));
        assertServiceException(() -> tenantService.validTenant(tenant.getId()), TENANT_EXPIRE);
    }

    @Test
    void validTenant_正常() {
        TenantDO tenant = insertTenant("正常租户", null, 99L, t -> {});
        tenantService.validTenant(tenant.getId());
    }

    @Test
    void getTenantIdList_只返回开启且未过期的租户() {
        TenantDO ok = insertTenant("正常", null, 99L, t -> {});
        TenantDO disabled = insertTenant("禁用", null, 99L, t -> t.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        TenantDO expired = insertTenant("过期", null, 99L, t -> t.setExpireTime(LocalDateTime.now().minusDays(1)));

        List<Long> ids = tenantService.getTenantIdList();

        assertThat(ids).contains(ok.getId()).doesNotContain(disabled.getId(), expired.getId());
    }

    // ========== 创建 ==========

    @Test
    void createTenant_成功时应同时创建租户管理员角色与账号() {
        TenantPackageDO pkg = insertPackage("基础版", Set.of(1L, 2L, 3L), CommonStatusEnum.ENABLE);
        TenantSaveReqVO reqVO = newSaveReqVO("新租户", "new.ccloud.local", pkg.getId());

        Long tenantId = tenantService.createTenant(reqVO);

        TenantDO tenant = tenantMapper.selectById(tenantId);
        assertThat(tenant.getName()).isEqualTo("新租户");
        assertThat(tenant.getContactUserId()).isNotNull();
        TenantUtils.execute(tenantId, () -> {
            List<RoleDO> roles = roleMapper.selectList();
            assertThat(roles).singleElement().satisfies(role -> {
                assertThat(role.getCode()).isEqualTo(RoleCodeEnum.TENANT_ADMIN.getCode());
                assertThat(role.getType()).isEqualTo(RoleTypeEnum.SYSTEM.getType());
                assertThat(role.getTenantId()).isEqualTo(tenantId);
                assertThat(permissionService.getRoleMenuListByRoleId(role.getId())).containsExactlyInAnyOrder(1L, 2L, 3L);
            });
            AdminUserDO user = userMapper.selectById(tenant.getContactUserId());
            assertThat(user.getUsername()).isEqualTo("tenantadmin");
            assertThat(user.getTenantId()).isEqualTo(tenantId);
            assertThat(passwordEncoder.matches("admin123", user.getPassword())).isTrue();
            assertThat(permissionService.getUserRoleIdListByUserId(user.getId()))
                    .containsExactly(roles.get(0).getId());
        });
        // 当前租户（1）内不应出现新租户的数据
        assertThat(roleMapper.selectCount(RoleDO::getCode, RoleCodeEnum.TENANT_ADMIN.getCode())).isZero();
    }

    @Test
    void createTenant_名称重复() {
        insertTenant("重名", null, 99L, t -> {});
        TenantPackageDO pkg = insertPackage("基础版", Set.of(1L), CommonStatusEnum.ENABLE);
        assertServiceException(() -> tenantService.createTenant(newSaveReqVO("重名", null, pkg.getId())),
                TENANT_NAME_DUPLICATE);
    }

    @Test
    void createTenant_域名重复() {
        insertTenant("甲", "dup.ccloud.local", 99L, t -> {});
        TenantPackageDO pkg = insertPackage("基础版", Set.of(1L), CommonStatusEnum.ENABLE);
        assertServiceException(() -> tenantService.createTenant(newSaveReqVO("乙", "dup.ccloud.local", pkg.getId())),
                TENANT_WEBSITE_DUPLICATE);
    }

    @Test
    void createTenant_套餐不存在() {
        assertServiceException(() -> tenantService.createTenant(newSaveReqVO("无套餐", null, -1L)),
                TENANT_PACKAGE_NOT_EXISTS);
        assertThat(tenantMapper.selectCount(TenantDO::getName, "无套餐")).isZero();
    }

    @Test
    void createTenant_套餐已禁用() {
        TenantPackageDO pkg = insertPackage("停用版", Set.of(1L), CommonStatusEnum.DISABLE);
        assertServiceException(() -> tenantService.createTenant(newSaveReqVO("甲", null, pkg.getId())),
                TENANT_PACKAGE_DISABLE);
    }

    /**
     * 不包在测试事务里，才能观察到业务事务真正回滚：
     * 账号配额为 0 使得"建租户、建角色"成功之后"建账号"失败，前两步必须一并撤销。
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void createTenant_创建账号失败时租户与角色应一并回滚() {
        TenantPackageDO pkg = TenantUtils.executeIgnore(() -> insertPackage("回滚版", Set.of(1L), CommonStatusEnum.ENABLE));
        TenantSaveReqVO reqVO = newSaveReqVO("回滚租户", null, pkg.getId());
        reqVO.setAccountCount(0);
        try {
            assertServiceException(() -> tenantService.createTenant(reqVO), USER_COUNT_MAX);

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM system_tenant WHERE name = '回滚租户'", Integer.class)).isZero();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM system_role r LEFT JOIN system_tenant t ON r.tenant_id = t.id "
                            + "WHERE r.code = 'tenant_admin' AND t.id IS NULL", Integer.class)).isZero();
        } finally {
            jdbcTemplate.update("DELETE FROM system_tenant WHERE name = '回滚租户'");
            jdbcTemplate.update("DELETE FROM system_tenant_package WHERE id = ?", pkg.getId());
        }
    }

    // ========== 修改 ==========

    @Test
    void updateTenant_变更套餐后应同步租户下角色的菜单() {
        TenantPackageDO oldPkg = insertPackage("旧版", Set.of(1L, 2L, 3L), CommonStatusEnum.ENABLE);
        TenantPackageDO newPkg = insertPackage("新版", Set.of(2L, 3L, 4L), CommonStatusEnum.ENABLE);
        Long tenantId = tenantService.createTenant(newSaveReqVO("换套餐", null, oldPkg.getId()));
        Long customRoleId = TenantUtils.execute(tenantId, () -> {
            RoleDO role = new RoleDO().setName("客服").setCode("service").setSort(1)
                    .setStatus(CommonStatusEnum.ENABLE.getStatus()).setType(RoleTypeEnum.CUSTOM.getType()).setDataScope(1);
            roleMapper.insert(role);
            permissionService.assignRoleMenu(role.getId(), Set.of(1L, 3L));
            return role.getId();
        });

        TenantSaveReqVO reqVO = newSaveReqVO("换套餐", null, newPkg.getId());
        reqVO.setId(tenantId);
        tenantService.updateTenant(reqVO);

        assertThat(tenantMapper.selectById(tenantId).getPackageId()).isEqualTo(newPkg.getId());
        TenantUtils.execute(tenantId, () -> {
            RoleDO admin = roleMapper.selectOne(RoleDO::getCode, RoleCodeEnum.TENANT_ADMIN.getCode());
            // 租户管理员：与新套餐完全一致
            assertThat(permissionService.getRoleMenuListByRoleId(admin.getId())).containsExactlyInAnyOrder(2L, 3L, 4L);
            // 自定义角色：只收回超出新套餐的菜单，不自动授予新增菜单
            assertThat(permissionService.getRoleMenuListByRoleId(customRoleId)).containsExactly(3L);
        });
    }

    @Test
    void updateTenant_不存在() {
        TenantSaveReqVO reqVO = newSaveReqVO("x", null, 1L);
        reqVO.setId(-1L);
        assertServiceException(() -> tenantService.updateTenant(reqVO), TENANT_NOT_EXISTS);
    }

    @Test
    void updateTenant_系统租户不允许修改() {
        TenantDO system = insertTenant("系统", null, TenantDO.PACKAGE_ID_SYSTEM, t -> {});
        TenantSaveReqVO reqVO = newSaveReqVO("系统2", null, 1L);
        reqVO.setId(system.getId());
        assertServiceException(() -> tenantService.updateTenant(reqVO), TENANT_CAN_NOT_UPDATE_SYSTEM);
    }

    @Test
    void updateTenant_名称与其他租户重复() {
        TenantPackageDO pkg = insertPackage("版", Set.of(1L), CommonStatusEnum.ENABLE);
        insertTenant("已占用", null, pkg.getId(), t -> {});
        TenantDO self = insertTenant("自己", null, pkg.getId(), t -> {});
        TenantSaveReqVO reqVO = newSaveReqVO("已占用", null, pkg.getId());
        reqVO.setId(self.getId());
        assertServiceException(() -> tenantService.updateTenant(reqVO), TENANT_NAME_DUPLICATE);
    }

    // ========== 删除 ==========

    @Test
    void deleteTenant_成功() {
        TenantDO tenant = insertTenant("待删", null, 99L, t -> {});
        tenantService.deleteTenant(tenant.getId());
        assertThat(tenantMapper.selectById(tenant.getId())).isNull();
    }

    @Test
    void deleteTenant_系统租户不允许删除() {
        TenantDO system = insertTenant("系统", null, TenantDO.PACKAGE_ID_SYSTEM, t -> {});
        assertServiceException(() -> tenantService.deleteTenant(system.getId()), TENANT_CAN_NOT_UPDATE_SYSTEM);
    }

    @Test
    void deleteTenant_不存在() {
        assertServiceException(() -> tenantService.deleteTenant(-1L), TENANT_NOT_EXISTS);
    }

    // ========== 查询 ==========

    @Test
    void getTenantPage_按条件过滤() {
        insertTenant("华东分公司", null, 99L, t -> t.setContactName("张三"));
        insertTenant("华南分公司", null, 99L, t -> t.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        insertTenant("其他", null, 99L, t -> {});

        TenantPageReqVO reqVO = new TenantPageReqVO();
        reqVO.setName("分公司");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        PageResult<TenantDO> page = tenantService.getTenantPage(reqVO);

        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getList()).extracting(TenantDO::getName).containsExactly("华东分公司");
    }

    @Test
    void getTenantByName与getTenantByWebsite() {
        TenantDO tenant = insertTenant("按名查", "find.ccloud.local", 99L, t -> {});
        assertThat(tenantService.getTenantByName("按名查").getId()).isEqualTo(tenant.getId());
        assertThat(tenantService.getTenantByWebsite("find.ccloud.local").getId()).isEqualTo(tenant.getId());
        assertThat(tenantService.getTenantByName("不存在")).isNull();
    }

    @Test
    void getTenantListByPackageId与计数() {
        TenantPackageDO pkg = insertPackage("计数版", Set.of(1L), CommonStatusEnum.ENABLE);
        insertTenant("甲", null, pkg.getId(), t -> {});
        insertTenant("乙", null, pkg.getId(), t -> {});
        assertThat(tenantService.getTenantCountByPackageId(pkg.getId())).isEqualTo(2);
        assertThat(tenantService.getTenantListByPackageId(pkg.getId())).hasSize(2);
    }

    @Test
    void handleTenantInfo_应传入当前租户() {
        TenantDO tenant = insertTenant("当前", null, 99L, t -> {});
        List<String> names = new ArrayList<>();
        TenantUtils.execute(tenant.getId(), () -> tenantService.handleTenantInfo(t -> names.add(t.getName())));
        assertThat(names).containsExactly("当前");
    }

    @Test
    void handleTenantMenu_普通租户传入套餐菜单_系统租户不受限() {
        TenantPackageDO pkg = insertPackage("菜单版", Set.of(7L, 8L), CommonStatusEnum.ENABLE);
        TenantDO normal = insertTenant("普通", null, pkg.getId(), t -> {});
        TenantDO system = insertTenant("系统", null, TenantDO.PACKAGE_ID_SYSTEM, t -> {});

        List<Set<Long>> received = new ArrayList<>();
        TenantUtils.execute(normal.getId(), () -> tenantService.handleTenantMenu(received::add));
        assertThat(received).containsExactly(Set.of(7L, 8L));

        received.clear();
        TenantUtils.execute(system.getId(), () -> tenantService.handleTenantMenu(received::add));
        assertThat(received).isEmpty();
    }

    // ========== 工具 ==========

    private TenantDO insertTenant(String name, String website, Long packageId,
                                  java.util.function.Consumer<TenantDO> customizer) {
        TenantDO tenant = new TenantDO().setName(name).setContactName("联系人").setContactMobile("13900000000")
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setWebsite(website).setPackageId(packageId)
                .setExpireTime(LocalDateTime.now().plusYears(1)).setAccountCount(10);
        customizer.accept(tenant);
        tenantMapper.insert(tenant);
        return tenant;
    }

    private TenantPackageDO insertPackage(String name, Set<Long> menuIds, CommonStatusEnum status) {
        TenantPackageDO pkg = new TenantPackageDO().setName(name).setStatus(status.getStatus())
                .setRemark("").setMenuIds(menuIds);
        tenantPackageMapper.insert(pkg);
        return pkg;
    }

    private static TenantSaveReqVO newSaveReqVO(String name, String website, Long packageId) {
        TenantSaveReqVO reqVO = new TenantSaveReqVO();
        reqVO.setName(name);
        reqVO.setContactName("租户联系人");
        reqVO.setContactMobile("13911112222");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setWebsite(website);
        reqVO.setPackageId(packageId);
        reqVO.setExpireTime(LocalDateTime.now().plusYears(1));
        reqVO.setAccountCount(10);
        reqVO.setUsername("tenantadmin");
        reqVO.setPassword("admin123");
        return reqVO;
    }
}
