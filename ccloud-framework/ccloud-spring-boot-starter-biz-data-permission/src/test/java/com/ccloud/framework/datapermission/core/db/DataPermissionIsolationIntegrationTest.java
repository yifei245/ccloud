package com.ccloud.framework.datapermission.core.db;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.datapermission.core.annotation.DataPermission;
import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionFrameworkService;
import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionRespDTO;
import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCustomizer;
import com.ccloud.framework.datapermission.core.util.DataPermissionUtils;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.*;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据权限的端到端验证。
 *
 * <p>与多租户同理：断言"规则对象被注册了"毫无意义，必须断言<b>查询实际返回了哪些行</b>。
 * 数据权限失效同样是静默的——SQL 正常执行，只是本该看不到的行被返回了。
 */
@SpringBootTest(classes = DataPermissionIsolationIntegrationTest.TestApp.class)
@ActiveProfiles("unit-test")
class DataPermissionIsolationIntegrationTest {

    @Configuration
    @EnableAutoConfiguration
    @MapperScan("com.ccloud.framework.datapermission.core.db")
    static class TestApp {
        /**
         * 登记"哪张表按哪个字段做部门/本人过滤"
         */
        @Bean
        DeptDataPermissionRuleCustomizer demoCustomizer() {
            return rule -> {
                rule.addDeptColumn(DemoUserDO.class, "dept_id");
                rule.addUserColumn(DemoUserDO.class, "id");
            };
        }

        @Bean
        DemoUserService demoUserService() {
            return new DemoUserService();
        }

        /**
         * 用可控桩而非 @MockBean：@MockBean 的定义注册时机与
         * @ConditionalOnBean 的求值时机不保证先后，规则 Bean 可能因此根本不被创建，
         * 表现为"数据权限静默失效"——恰恰是本测试要防的那种故障。
         */
        @Bean
        StubPermissionService deptDataPermissionFrameworkService() {
            return new StubPermissionService();
        }
    }

    public static class StubPermissionService implements DeptDataPermissionFrameworkService {

        private volatile DeptDataPermissionRespDTO value = new DeptDataPermissionRespDTO().setAll(true);

        public void setValue(DeptDataPermissionRespDTO value) {
            this.value = value;
        }

        @Override
        public DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
            return value;
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @TableName("demo_user")
    public static class DemoUserDO extends BaseDO {
        private Long id;
        private String name;
        private Long deptId;
    }

    @Mapper
    public interface DemoUserMapper extends BaseMapperX<DemoUserDO> {
    }

    /**
     * {@code @DataPermission} 必须标在 <b>Spring 管理的 Bean</b> 的方法上。
     * 标在 Mapper 的 default 方法上无效——MyBatis 用自己的动态代理生成 Mapper 实例，
     * Spring AOP 的切面根本织不进去，注解会被静默忽略。
     */
    public static class DemoUserService {

        @Autowired
        private DemoUserMapper mapper;

        @DataPermission
        public java.util.List<DemoUserDO> selectVisible() {
            return mapper.selectList();
        }

        /**
         * 未加注解，不参与数据权限改写
         */
        public java.util.List<DemoUserDO> selectAllWithoutPermission() {
            return mapper.selectList();
        }
    }

    @Autowired
    private DemoUserMapper mapper;

    @Autowired
    private DemoUserService service;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private StubPermissionService permissionService;

    @BeforeEach
    void seed() {
        // 必须物理删除：BaseDO 开了逻辑删除，mapper.delete 只会把 deleted 置为真，
        // 行仍在表里，下一个用例重插相同主键会撞唯一约束
        jdbcTemplate.execute("DELETE FROM demo_user");
        DataPermissionUtils.executeIgnore(() -> {
            mapper.insert(newUser(1L, "张三", 10L));
            mapper.insert(newUser(2L, "李四", 10L));
            mapper.insert(newUser(3L, "王五", 20L));
            mapper.insert(newUser(4L, "赵六", 30L));
        });
        login(1L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private DemoUserDO newUser(Long id, String name, Long deptId) {
        DemoUserDO user = new DemoUserDO();
        user.setId(id);
        user.setName(name);
        user.setDeptId(deptId);
        return user;
    }

    private void login(Long userId) {
        LoginUser user = new LoginUser();
        user.setId(userId);
        user.setUserType(UserTypeEnum.ADMIN.getValue());
        user.setTenantId(1L);
        user.setExpiresTime(LocalDateTime.now().plusHours(1));
        SecurityFrameworkUtils.setLoginUser(user, null);
    }

    @Test
    @DisplayName("全部数据权限应能看到所有行")
    void allScope_shouldSeeEveryRow() {
        permissionService.setValue(new DeptDataPermissionRespDTO().setAll(true));
        assertThat(service.selectVisible()).hasSize(4);
    }

    @Test
    @DisplayName("本部门权限只能看到该部门的行")
    void deptScope_shouldOnlySeeOwnDept() {
        permissionService.setValue(new DeptDataPermissionRespDTO().setDeptIds(Set.of(10L)));
        assertThat(service.selectVisible()).extracting(DemoUserDO::getName)
                .containsExactlyInAnyOrder("张三", "李四");
    }

    @Test
    @DisplayName("多部门权限应能看到全部所属部门的行")
    void multiDeptScope_shouldSeeAllGrantedDepts() {
        permissionService.setValue(new DeptDataPermissionRespDTO().setDeptIds(Set.of(10L, 20L)));
        assertThat(service.selectVisible()).hasSize(3);
    }

    @Test
    @DisplayName("仅本人权限只能看到自己那一行")
    void selfScope_shouldOnlySeeOwnRow() {
        permissionService.setValue(new DeptDataPermissionRespDTO().setSelf(true));
        assertThat(service.selectVisible()).extracting(DemoUserDO::getName).containsExactly("张三");
    }

    @Test
    @DisplayName("部门与本人权限并存时取并集")
    void deptAndSelfScope_shouldSeeUnion() {
        login(4L);
        permissionService.setValue(new DeptDataPermissionRespDTO().setDeptIds(Set.of(10L)).setSelf(true));
        assertThat(service.selectVisible()).extracting(DemoUserDO::getName)
                .containsExactlyInAnyOrder("张三", "李四", "赵六");
    }

    @Test
    @DisplayName("无任何数据权限时一行都看不到，而不是看到全部")
    void noScope_shouldSeeNothing() {
        permissionService.setValue(new DeptDataPermissionRespDTO());
        assertThat(service.selectVisible()).isEmpty();
    }

    @Test
    @DisplayName("未加 @DataPermission 的查询不受影响，避免数据权限意外波及内部查询")
    void queryWithoutAnnotation_shouldNotBeFiltered() {
        permissionService.setValue(new DeptDataPermissionRespDTO().setSelf(true));
        assertThat(service.selectAllWithoutPermission()).hasSize(4);
    }

    @Test
    @DisplayName("executeIgnore 内的查询应绕过数据权限，供系统级操作使用")
    void executeIgnore_shouldBypassFiltering() {
        permissionService.setValue(new DeptDataPermissionRespDTO());
        assertThat(DataPermissionUtils.executeIgnore(() -> service.selectVisible())).hasSize(4);
    }
}
