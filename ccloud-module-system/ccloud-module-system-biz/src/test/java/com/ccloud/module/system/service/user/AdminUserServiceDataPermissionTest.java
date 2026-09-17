package com.ccloud.module.system.service.user;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.datapermission.config.CcloudDataPermissionAutoConfiguration;
import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionFrameworkService;
import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionRespDTO;
import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.system.controller.admin.user.vo.user.UserPageReqVO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.dal.mysql.user.AdminUserMapper;
import com.ccloud.module.system.framework.datapermission.config.DataPermissionConfiguration;
import com.ccloud.module.system.service.dept.DeptServiceImpl;
import com.ccloud.module.system.service.dept.PostServiceImpl;
import com.ccloud.module.system.service.permission.PermissionServiceImpl;
import com.ccloud.module.system.service.tenant.TenantService;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户分页必须受部门数据权限约束：断言实际返回的行，而不是断言规则被注册了
 */
@ImportAutoConfiguration(CcloudDataPermissionAutoConfiguration.class)
@Import({AdminUserServiceImpl.class, DeptServiceImpl.class, PostServiceImpl.class, PermissionServiceImpl.class,
        DataPermissionConfiguration.class, AdminUserServiceDataPermissionTest.StubConfig.class,
        org.springframework.validation.beanvalidation.LocalValidatorFactoryBean.class})
class AdminUserServiceDataPermissionTest extends BaseSystemDbUnitTest {

    static class StubConfig {
        @Bean
        StubDeptDataPermissionService stubDeptDataPermissionService() {
            return new StubDeptDataPermissionService();
        }
    }

    static class StubDeptDataPermissionService implements DeptDataPermissionFrameworkService {
        volatile DeptDataPermissionRespDTO value = new DeptDataPermissionRespDTO().setAll(true);

        @Override
        public DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
            return value;
        }
    }

    @Resource
    private AdminUserService userService;
    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private StubDeptDataPermissionService stub;

    @MockitoBean
    private TenantService tenantService;

    private Long selfId;

    @BeforeEach
    void seed() {
        selfId = insertUser("self", 10L);
        insertUser("colleague", 10L);
        insertUser("stranger", 20L);
        LoginUser loginUser = new LoginUser();
        loginUser.setId(selfId);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        loginUser.setTenantId(TENANT_ID);
        loginUser.setExpiresTime(LocalDateTime.now().plusHours(1));
        SecurityFrameworkUtils.setLoginUser(loginUser, null);
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 全部数据权限可见全部用户() {
        stub.value = new DeptDataPermissionRespDTO().setAll(true);
        assertThat(userService.getUserPage(new UserPageReqVO()).getTotal()).isEqualTo(3);
    }

    @Test
    void 本部门数据权限只见同部门用户_分页总数同样受限() {
        stub.value = new DeptDataPermissionRespDTO().setDeptIds(Set.of(10L));
        var page = userService.getUserPage(new UserPageReqVO());
        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getList()).extracting(AdminUserDO::getUsername).containsExactlyInAnyOrder("self", "colleague");
    }

    @Test
    void 仅本人数据权限只见自己() {
        stub.value = new DeptDataPermissionRespDTO().setSelf(true);
        assertThat(userService.getUserPage(new UserPageReqVO()).getList())
                .extracting(AdminUserDO::getUsername).containsExactly("self");
    }

    @Test
    void 非分页查询不受数据权限影响() {
        stub.value = new DeptDataPermissionRespDTO().setSelf(true);
        assertThat(userService.getUserByUsername("stranger")).isNotNull();
    }

    private Long insertUser(String username, Long deptId) {
        AdminUserDO user = new AdminUserDO().setUsername(username).setPassword("x").setNickname(username)
                .setDeptId(deptId).setStatus(0);
        userMapper.insert(user);
        return user.getId();
    }
}
