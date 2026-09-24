package com.ccloud.module.member;

import com.ccloud.framework.common.enums.TerminalEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.security.core.filter.LoginUserFilter;
import com.ccloud.framework.security.core.util.InternalSignUtils;
import com.ccloud.framework.test.core.ut.MySqlTestContainer;
import com.ccloud.module.system.api.logger.LoginLogApi;
import com.ccloud.module.system.api.oauth2.OAuth2TokenApi;
import com.ccloud.module.system.api.oauth2.dto.OAuth2AccessTokenCreateReqDTO;
import com.ccloud.module.system.api.oauth2.dto.OAuth2AccessTokenRespDTO;
import com.ccloud.module.system.api.sms.SmsCodeApi;
import com.ccloud.module.system.api.tenant.TenantApi;
import com.ccloud.module.system.enums.oauth2.OAuth2ClientConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 从 HTTP 入口验证 C 端的鉴权边界：哪些接口可匿名、身份的用户类型、请求头的解析。
 * system 的 RPC 以桩替代；身份头按网关的方式签名
 */
@SpringBootTest(classes = MemberServerApplication.class,
        properties = {
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.config.import-check.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false"
        })
@AutoConfigureMockMvc
@ActiveProfiles("unit-test")
class MemberSecurityIntegrationTest {

    private static final String SECRET = "unit-test-secret";
    private static final long TENANT = 1L;
    private static final long MEMBER_ID = 90101L;

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS = startRedis();

    private static GenericContainer<?> startRedis() {
        GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
        redis.start();
        return redis;
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        var mysql = MySqlTestContainer.get();
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

    @MockitoBean
    private TenantApi tenantApi;
    @MockitoBean
    private OAuth2TokenApi oauth2TokenApi;
    @MockitoBean
    private SmsCodeApi smsCodeApi;
    @MockitoBean
    private LoginLogApi loginLogApi;

    @BeforeEach
    void setUp() {
        cleanUp();
        jdbcTemplate.update("INSERT INTO member_user (id, mobile, password, status, register_ip, nickname, tenant_id) "
                + "VALUES (?, '13933330001', '$2a$04$hash', 0, '127.0.0.1', '会员甲', ?)", MEMBER_ID, TENANT);
        when(tenantApi.validTenant(anyLong())).thenReturn(CommonResult.success(true));
        when(smsCodeApi.useSmsCode(any())).thenReturn(CommonResult.success(true));
        when(loginLogApi.createLoginLog(any())).thenReturn(CommonResult.success(true));
        when(oauth2TokenApi.createAccessToken(any())).thenAnswer(invocation -> {
            OAuth2AccessTokenCreateReqDTO req = invocation.getArgument(0);
            OAuth2AccessTokenRespDTO dto = new OAuth2AccessTokenRespDTO();
            dto.setAccessToken("at-" + req.getClientId());
            dto.setUserId(req.getUserId());
            dto.setUserType(req.getUserType());
            return CommonResult.success(dto);
        });
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM member_user WHERE id = ? OR mobile LIKE '139333300%'", MEMBER_ID);
    }

    // ========== 需要登录的接口 ==========

    @Test
    void 匿名访问需登录的认证接口应返回401() throws Exception {
        mockMvc.perform(post("/app-api/member/auth/update-password").header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"1234\",\"password\":\"new123\"}"))
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/app-api/member/auth/get-permission-info").header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 匿名访问用户接口应返回401() throws Exception {
        mockMvc.perform(get("/app-api/member/user/get").header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 管理员身份访问会员接口应返回401_不能冒充同编号的会员() throws Exception {
        mockMvc.perform(asUser(get("/app-api/member/user/get"), MEMBER_ID, UserTypeEnum.ADMIN))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 会员身份获取本人信息_不返回密码() throws Exception {
        mockMvc.perform(asUser(get("/app-api/member/user/get"), MEMBER_ID, UserTypeEnum.MEMBER))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(MEMBER_ID))
                .andExpect(jsonPath("$.data.mobile").value("13933330001"))
                .andExpect(jsonPath("$.data.passwordSet").value(true))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    // ========== 匿名接口与请求头 ==========

    @Test
    void 短信登录可匿名调用_客户端与终端取自请求头() throws Exception {
        mockMvc.perform(post("/app-api/member/auth/sms-login").header("X-Tenant-Id", TENANT)
                        .header(OAuth2ClientConstants.HEADER_CLIENT_ID, OAuth2ClientConstants.CLIENT_ID_H5)
                        .header("X-Terminal", TerminalEnum.H5.getTerminal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"13933330002\",\"code\":\"1234\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("at-ccloud-h5"))
                .andExpect(jsonPath("$.data.userType").value(UserTypeEnum.MEMBER.getValue()));

        assertThat(jdbcTemplate.queryForObject("SELECT register_terminal FROM member_user WHERE mobile = '13933330002'",
                Integer.class)).isEqualTo(TerminalEnum.H5.getTerminal());
    }

    @Test
    void 未指定客户端时默认ccloud_app() throws Exception {
        mockMvc.perform(post("/app-api/member/auth/sms-login").header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"13933330003\",\"code\":\"1234\"}"))
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<OAuth2AccessTokenCreateReqDTO> captor = ArgumentCaptor.forClass(OAuth2AccessTokenCreateReqDTO.class);
        verify(oauth2TokenApi).createAccessToken(captor.capture());
        assertThat(captor.getValue().getClientId()).isEqualTo(OAuth2ClientConstants.CLIENT_ID_APP);
    }

    @Test
    void 指定管理后台客户端应拒绝() throws Exception {
        mockMvc.perform(post("/app-api/member/auth/sms-login").header("X-Tenant-Id", TENANT)
                        .header(OAuth2ClientConstants.HEADER_CLIENT_ID, OAuth2ClientConstants.CLIENT_ID_WEB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"13933330004\",\"code\":\"1234\"}"))
                .andExpect(jsonPath("$.code").value(400));
        verify(oauth2TokenApi, never()).createAccessToken(any());
    }

    @Test
    void 登出可匿名调用_作废请求携带的令牌() throws Exception {
        when(oauth2TokenApi.removeAccessToken("at-1")).thenReturn(CommonResult.success(null));

        mockMvc.perform(post("/app-api/member/auth/logout").header("X-Tenant-Id", TENANT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer at-1"))
                .andExpect(jsonPath("$.code").value(0));

        verify(oauth2TokenApi).removeAccessToken("at-1");
    }

    // ========== RPC ==========

    @Test
    void RPC获取会员用户不应返回密码() throws Exception {
        mockMvc.perform(get("/rpc-api/member/user/get").param("id", String.valueOf(MEMBER_ID))
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("会员甲"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder, long userId,
                                                 UserTypeEnum userType) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(userType.getValue());
        loginUser.setTenantId(TENANT);
        loginUser.setExpiresTime(LocalDateTime.now().plusHours(1));
        String payload = Base64.getEncoder().encodeToString(
                JsonUtils.toJsonString(loginUser).getBytes(StandardCharsets.UTF_8));
        long timestamp = System.currentTimeMillis();
        return builder.header(LoginUserFilter.HEADER_LOGIN_USER, payload)
                .header(LoginUserFilter.HEADER_LOGIN_USER_SIGN, InternalSignUtils.sign(payload, timestamp, SECRET))
                .header(LoginUserFilter.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header("X-Tenant-Id", TENANT);
    }
}
