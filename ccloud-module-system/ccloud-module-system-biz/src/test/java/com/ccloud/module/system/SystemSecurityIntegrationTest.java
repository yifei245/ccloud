package com.ccloud.module.system;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.datapermission.core.rule.DataPermissionRule;
import com.ccloud.framework.excel.core.service.DictFrameworkService;
import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.security.core.filter.LoginUserFilter;
import com.ccloud.framework.security.core.util.InternalSignUtils;
import com.ccloud.framework.test.core.ut.MySqlTestContainer;
import com.ccloud.module.system.enums.ErrorCodeConstants;
import com.ccloud.module.system.enums.permission.RoleCodeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 真实装配下的安全链路：权限表达式、租户校验、数据权限规则是否真正生效。
 *
 * <p>单元测试直接调用 Service，框架契约（权限、租户、数据权限、字典）缺实现时照样全绿；
 * 只有从 HTTP 入口走完整过滤器链才能发现。身份头按网关的方式签名。
 */
@SpringBootTest(classes = SystemServerApplication.class,
        properties = {
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.config.import-check.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false"
        })
@AutoConfigureMockMvc
@ActiveProfiles("unit-test")
class SystemSecurityIntegrationTest {

    private static final String SECRET = "unit-test-secret";

    private static final long TENANT = 90001L;
    private static final long OTHER_TENANT = 90002L;
    private static final long EXPIRED_TENANT = 90003L;
    private static final long PLAIN_USER = 90011L;
    private static final long SUPER_ADMIN = 90012L;
    private static final long EXPIRED_TENANT_USER = 90013L;
    private static final long SUPER_ADMIN_ROLE = 90021L;

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS = startRedis();

    private static GenericContainer<?> startRedis() {
        GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
        redis.start();
        return redis;
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        MySQLContainer<?> mysql = MySqlTestContainer.get();
        registry.add("spring.datasource.dynamic.datasource.master.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.dynamic.datasource.master.username", mysql::getUsername);
        registry.add("spring.datasource.dynamic.datasource.master.password", mysql::getPassword);
        registry.add("spring.datasource.dynamic.datasource.master.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("ccloud.security.internal-sign-secret", () -> SECRET);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ApplicationContext ctx;

    @BeforeEach
    void seed() {
        cleanUp();
        insertTenant(TENANT, "安全测试租户", LocalDateTime.now().plusDays(30));
        insertTenant(OTHER_TENANT, "另一个租户", LocalDateTime.now().plusDays(30));
        insertTenant(EXPIRED_TENANT, "已过期租户", LocalDateTime.now().minusDays(1));
        insertUser(PLAIN_USER, "sec_plain", TENANT);
        insertUser(SUPER_ADMIN, "sec_admin", TENANT);
        insertUser(EXPIRED_TENANT_USER, "sec_expired", EXPIRED_TENANT);
        jdbcTemplate.update("INSERT INTO system_role (id, name, code, sort, data_scope, data_scope_dept_ids, status, type, tenant_id) "
                + "VALUES (?, '超级管理员', ?, 0, 1, '', 0, 1, ?)", SUPER_ADMIN_ROLE, RoleCodeEnum.SUPER_ADMIN.getCode(), TENANT);
        jdbcTemplate.update("INSERT INTO system_user_role (user_id, role_id, tenant_id) VALUES (?, ?, ?)",
                SUPER_ADMIN, SUPER_ADMIN_ROLE, TENANT);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM system_user_role WHERE user_id BETWEEN 90000 AND 90099");
        jdbcTemplate.update("DELETE FROM system_role WHERE id BETWEEN 90000 AND 90099");
        jdbcTemplate.update("DELETE FROM system_users WHERE id BETWEEN 90000 AND 90099");
        jdbcTemplate.update("DELETE FROM system_tenant WHERE id BETWEEN 90000 AND 90099");
    }

    // ========== 权限表达式 @ss ==========

    @Test
    void 没有权限的用户访问受保护接口应被拒绝() throws Exception {
        mockMvc.perform(asUser(get("/admin-api/system/user/page"), PLAIN_USER, TENANT))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void 超级管理员访问受保护接口应放行() throws Exception {
        mockMvc.perform(asUser(get("/admin-api/system/user/page"), SUPER_ADMIN, TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    // ========== 租户校验 ==========

    @Test
    void 登录用户改请求头访问其他租户应被拒绝() throws Exception {
        mockMvc.perform(asUser(get("/admin-api/system/user/page"), SUPER_ADMIN, TENANT, OTHER_TENANT))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("您无权访问该租户的数据"));
    }

    @Test
    void 匿名接口未携带租户编号应被拒绝_否则会跨全部租户查询() throws Exception {
        mockMvc.perform(post("/admin-api/system/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sec_admin\",\"password\":\"admin123\"}"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("请求未携带租户编号"));
    }

    @Test
    void 已过期租户的请求应被拒绝() throws Exception {
        mockMvc.perform(asUser(get("/admin-api/system/user/page"), EXPIRED_TENANT_USER, EXPIRED_TENANT))
                .andExpect(jsonPath("$.code").value(ErrorCodeConstants.TENANT_EXPIRE.getCode()));
    }

    // ========== 其余框架契约 ==========

    @Test
    void 部门数据权限规则与字典翻译应被装配() {
        assertThat(ctx.getBeansOfType(DataPermissionRule.class)).as("部门数据权限规则").isNotEmpty();
        assertThat(ctx.getBeansOfType(DictFrameworkService.class)).as("Excel 字典翻译").hasSize(1);
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder, long userId, long tenantId) {
        return asUser(builder, userId, tenantId, tenantId);
    }

    /**
     * @param headerTenantId 请求头声明的租户。MockMvc 的 header() 是追加而非覆盖，所以只能在这里一次性给定
     */
    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder, long userId, long tenantId,
                                                 long headerTenantId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        loginUser.setTenantId(tenantId);
        loginUser.setExpiresTime(LocalDateTime.now().plusHours(1));
        String payload = Base64.getEncoder().encodeToString(
                JsonUtils.toJsonString(loginUser).getBytes(StandardCharsets.UTF_8));
        long timestamp = System.currentTimeMillis();
        return builder.header(LoginUserFilter.HEADER_LOGIN_USER, payload)
                .header(LoginUserFilter.HEADER_LOGIN_USER_SIGN, InternalSignUtils.sign(payload, timestamp, SECRET))
                .header(LoginUserFilter.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header("X-Tenant-Id", headerTenantId);
    }

    private void insertTenant(long id, String name, LocalDateTime expireTime) {
        jdbcTemplate.update("INSERT INTO system_tenant (id, name, contact_name, status, package_id, expire_time, account_count) "
                + "VALUES (?, ?, '联系人', 0, 0, ?, 100)", id, name, expireTime);
    }

    private void insertUser(long id, String username, long tenantId) {
        jdbcTemplate.update("INSERT INTO system_users (id, username, nickname, password, status, tenant_id) "
                + "VALUES (?, ?, ?, '', 0, ?)", id, username, username, tenantId);
    }
}
