package com.ccloud.module.open.test;

import com.ccloud.framework.test.core.ut.LocalRedis;
import com.ccloud.framework.test.core.ut.LocalMySql;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.security.core.filter.LoginUserFilter;
import com.ccloud.framework.security.core.util.InternalSignUtils;
import com.ccloud.module.open.OpenServerApplication;
import com.ccloud.module.system.api.permission.PermissionApi;
import com.ccloud.module.system.api.tenant.TenantApi;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * 从 HTTP 入口测试 open 服务：真实 MySQL 与 Redis，system 的 RPC 以桩替代，身份头按网关的方式签名。
 * 所有 Web 测试继承本类，共用一个 Spring 上下文。
 *
 * <p>测试不回滚事务，数据会提交到共享的测试库：子类须在 {@code @AfterEach} 中删除自己插入的数据
 */
@SpringBootTest(classes = OpenServerApplication.class,
        properties = {
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.config.import-check.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false"
        })
@AutoConfigureMockMvc
@ActiveProfiles("unit-test")
public abstract class BaseOpenWebIntegrationTest {

    protected static final String SECRET = "unit-test-secret";
    protected static final long TENANT = 1L;
    /**
     * 普通租户，用于验证"仅限系统租户"的操作被拒绝
     */
    protected static final long TENANT_2 = 2L;
    /**
     * 拥有全部权限的管理员
     */
    protected static final long ADMIN_ID = 1L;
    /**
     * 没有任何权限的管理员
     */
    protected static final long NO_PERM_ADMIN_ID = 2L;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        LocalMySql.register(registry);
        LocalRedis.register(registry);
        registry.add("ccloud.security.internal-sign-secret", () -> SECRET);
    }

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @MockitoBean
    protected TenantApi tenantApi;
    @MockitoBean
    protected PermissionApi permissionApi;

    @BeforeEach
    void stubCommonApis() {
        when(tenantApi.validTenant(anyLong())).thenReturn(CommonResult.success(true));
        // 租户 1 为系统租户；验证普通租户被拒的用例以 TENANT_2 访问
        when(tenantApi.isSystemTenant(anyLong())).thenAnswer(inv ->
                CommonResult.success(Long.valueOf(1L).equals(inv.getArgument(0))));
        when(permissionApi.hasAnyPermissions(eq(ADMIN_ID), any())).thenReturn(CommonResult.success(true));
        when(permissionApi.hasAnyPermissions(eq(NO_PERM_ADMIN_ID), any())).thenReturn(CommonResult.success(false));
    }

    protected MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder builder, long userId) {
        return asAdminOfTenant(builder, userId, TENANT);
    }

    /**
     * 以指定租户的管理员访问；X-Tenant-Id 与身份中的租户一致
     */
    protected MockHttpServletRequestBuilder asAdminOfTenant(MockHttpServletRequestBuilder builder, long userId,
                                                            long tenantId) {
        return asUser(builder, userId, UserTypeEnum.ADMIN, tenantId);
    }

    protected MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder, long userId,
                                                   UserTypeEnum userType, long tenantId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(userType.getValue());
        loginUser.setTenantId(tenantId);
        loginUser.setExpiresTime(LocalDateTime.now().plusHours(1));
        String payload = Base64.getEncoder().encodeToString(
                JsonUtils.toJsonString(loginUser).getBytes(StandardCharsets.UTF_8));
        long timestamp = System.currentTimeMillis();
        return builder.header(LoginUserFilter.HEADER_LOGIN_USER, payload)
                .header(LoginUserFilter.HEADER_LOGIN_USER_SIGN, InternalSignUtils.sign(payload, timestamp, SECRET))
                .header(LoginUserFilter.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header("X-Tenant-Id", tenantId);
    }
}
