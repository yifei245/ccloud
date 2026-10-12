package com.ccloud.framework.security.core.filter;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.security.config.SecurityProperties;
import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.security.core.util.InternalSignUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 身份头解析过滤器。
 *
 * <p>所有身份断言都必须<b>在过滤器链内</b>完成：过滤器在 finally 中清理上下文，
 * 链外断言"身份为 null"永远成立，是一条什么都测不到的假测试。
 */
class LoginUserFilterTest {

    private static final String SECRET = "ccloud-internal-sign-secret-for-test";

    private LoginUserFilter filter;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties();
        properties.setInternalSignSecret(SECRET);
        filter = new LoginUserFilter(properties);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private LoginUser newLoginUser() {
        LoginUser user = new LoginUser();
        user.setId(100L);
        user.setUserType(UserTypeEnum.ADMIN.getValue());
        user.setTenantId(1L);
        user.setClientId("ccloud-web");
        user.setScopes(List.of("user.read"));
        user.setExpiresTime(LocalDateTime.now().plusHours(1));
        return user;
    }

    private static String encode(LoginUser user) {
        return Base64.getEncoder().encodeToString(
                JsonUtils.toJsonString(user).getBytes(StandardCharsets.UTF_8));
    }

    private MockHttpServletRequest request(String payload, String sign, long timestamp) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-api/system/user/page");
        if (payload != null) {
            request.addHeader(LoginUserFilter.HEADER_LOGIN_USER, payload);
            request.addHeader(LoginUserFilter.HEADER_LOGIN_USER_SIGN, sign);
            request.addHeader(LoginUserFilter.HEADER_TIMESTAMP, String.valueOf(timestamp));
        }
        return request;
    }

    /**
     * 执行过滤器，并捕获链内看到的登录身份与租户属性
     */
    private record Captured(LoginUser loginUser, Object tenantId, boolean chainInvoked) {}

    private Captured run(MockHttpServletRequest request) throws Exception {
        AtomicReference<LoginUser> user = new AtomicReference<>();
        AtomicReference<Object> tenant = new AtomicReference<>();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> {
            invoked.set(true);
            user.set(SecurityFrameworkUtils.getLoginUser());
            tenant.set(req.getAttribute(LoginUserFilter.REQUEST_ATTRIBUTE_LOGIN_USER_TENANT_ID));
        };
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return new Captured(user.get(), tenant.get(), invoked.get());
    }

    @Test
    @DisplayName("合法内部头应在链内建立身份，并把租户编号暴露给租户安全过滤器比对")
    void validHeader_shouldEstablishIdentityDuringChain() throws Exception {
        long ts = System.currentTimeMillis();
        String payload = encode(newLoginUser());
        Captured captured = run(request(payload, InternalSignUtils.sign(payload, ts, SECRET), ts));

        assertThat(captured.loginUser()).isNotNull();
        assertThat(captured.loginUser().getId()).isEqualTo(100L);
        assertThat(captured.loginUser().getUserType()).isEqualTo(UserTypeEnum.ADMIN.getValue());
        assertThat(captured.loginUser().getClientId()).isEqualTo("ccloud-web");
        assertThat(captured.loginUser().getScopes()).containsExactly("user.read");
        assertThat(captured.tenantId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("伪造签名的请求在链内必须没有身份，且请求仍要放行由后续鉴权返回 401")
    void forgedSignature_shouldNotEstablishIdentity() throws Exception {
        long ts = System.currentTimeMillis();
        Captured captured = run(request(encode(newLoginUser()), "forged-signature", ts));

        assertThat(captured.loginUser()).isNull();
        assertThat(captured.tenantId()).isNull();
        assertThat(captured.chainInvoked()).isTrue();
    }

    @Test
    @DisplayName("用他人密钥签出的头必须被拒绝")
    void signatureFromWrongSecret_shouldNotEstablishIdentity() throws Exception {
        long ts = System.currentTimeMillis();
        String payload = encode(newLoginUser());
        Captured captured = run(request(payload, InternalSignUtils.sign(payload, ts, "attacker-secret"), ts));

        assertThat(captured.loginUser()).isNull();
    }

    @Test
    @DisplayName("payload 被篡改后原签名应失效，防止改用户编号提权")
    void tamperedPayload_shouldNotEstablishIdentity() throws Exception {
        long ts = System.currentTimeMillis();
        String original = encode(newLoginUser());
        String sign = InternalSignUtils.sign(original, ts, SECRET);

        LoginUser elevated = newLoginUser();
        elevated.setId(1L);
        Captured captured = run(request(encode(elevated), sign, ts));

        assertThat(captured.loginUser()).isNull();
    }

    @Test
    @DisplayName("时间戳超出容差的请求不应建立身份，防止重放已签发的头")
    void staleTimestamp_shouldNotEstablishIdentity() throws Exception {
        long stale = System.currentTimeMillis() - Duration.ofMinutes(10).toMillis();
        String payload = encode(newLoginUser());
        Captured captured = run(request(payload, InternalSignUtils.sign(payload, stale, SECRET), stale));

        assertThat(captured.loginUser()).isNull();
    }

    @Test
    @DisplayName("令牌已过期的身份不应被接受，兜住网关缓存期内被吊销的令牌")
    void expiredLoginUser_shouldNotEstablishIdentity() throws Exception {
        LoginUser expired = newLoginUser();
        expired.setExpiresTime(LocalDateTime.now().minusMinutes(1));
        long ts = System.currentTimeMillis();
        String payload = encode(expired);
        Captured captured = run(request(payload, InternalSignUtils.sign(payload, ts, SECRET), ts));

        assertThat(captured.loginUser()).isNull();
    }

    @Test
    @DisplayName("未配置签名密钥时必须忽略全部身份头，绝不退化为无条件信任")
    void missingSecret_shouldRejectAllIdentities() throws Exception {
        SecurityProperties noSecret = new SecurityProperties();
        noSecret.setInternalSignSecret(null);
        filter = new LoginUserFilter(noSecret);

        long ts = System.currentTimeMillis();
        String payload = encode(newLoginUser());
        Captured captured = run(request(payload, InternalSignUtils.sign(payload, ts, SECRET), ts));

        assertThat(captured.loginUser()).isNull();
    }

    @Test
    @DisplayName("缺少签名或时间戳头时应忽略身份")
    void missingSignOrTimestampHeader_shouldNotEstablishIdentity() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-api/system/user/page");
        request.addHeader(LoginUserFilter.HEADER_LOGIN_USER, encode(newLoginUser()));

        assertThat(run(request).loginUser()).isNull();
    }

    @Test
    @DisplayName("无内部头时匿名放行，不抛异常")
    void noHeader_shouldPassThroughAnonymously() throws Exception {
        Captured captured = run(request(null, null, 0L));

        assertThat(captured.loginUser()).isNull();
        assertThat(captured.chainInvoked()).isTrue();
    }

    @Test
    @DisplayName("payload 不是合法 JSON 时应静默忽略而非 500")
    void malformedPayload_shouldBeIgnoredSilently() throws Exception {
        long ts = System.currentTimeMillis();
        String payload = Base64.getEncoder().encodeToString("not-json".getBytes(StandardCharsets.UTF_8));
        Captured captured = run(request(payload, InternalSignUtils.sign(payload, ts, SECRET), ts));

        assertThat(captured.loginUser()).isNull();
        assertThat(captured.chainInvoked()).isTrue();
    }

    @Test
    @DisplayName("管理员身份访问用户端接口应被忽略：两套用户编号会重复，放行即可冒充同编号的会员")
    void adminIdentityOnAppApi_shouldNotEstablishIdentity() throws Exception {
        long ts = System.currentTimeMillis();
        String payload = encode(newLoginUser());
        MockHttpServletRequest request = request(payload, InternalSignUtils.sign(payload, ts, SECRET), ts);
        request.setRequestURI("/app-api/member/user/get");

        Captured captured = run(request);

        assertThat(captured.loginUser()).isNull();
        assertThat(captured.chainInvoked()).isTrue();
    }

    @Test
    @DisplayName("会员身份访问管理端接口应被忽略")
    void memberIdentityOnAdminApi_shouldNotEstablishIdentity() throws Exception {
        LoginUser member = newLoginUser();
        member.setUserType(UserTypeEnum.MEMBER.getValue());
        long ts = System.currentTimeMillis();
        String payload = encode(member);

        assertThat(run(request(payload, InternalSignUtils.sign(payload, ts, SECRET), ts)).loginUser()).isNull();
    }

    @Test
    @DisplayName("会员身份访问用户端接口应建立身份")
    void memberIdentityOnAppApi_shouldEstablishIdentity() throws Exception {
        LoginUser member = newLoginUser();
        member.setUserType(UserTypeEnum.MEMBER.getValue());
        long ts = System.currentTimeMillis();
        String payload = encode(member);
        MockHttpServletRequest request = request(payload, InternalSignUtils.sign(payload, ts, SECRET), ts);
        request.setRequestURI("/app-api/member/user/get");

        assertThat(run(request).loginUser()).isNotNull();
    }

    private LoginUser filterAndGetLoginUser(String uri, LoginUser user) throws Exception {
        long ts = System.currentTimeMillis();
        String payload = encode(user);
        MockHttpServletRequest request = request(payload, InternalSignUtils.sign(payload, ts, SECRET), ts);
        request.setRequestURI(uri);
        return run(request).loginUser();
    }

    @Test
    @DisplayName("开放应用身份只在 /open-api 路径生效，其余路径一律忽略")
    void 开放应用身份只在open_api路径生效() throws Exception {
        LoginUser app = new LoginUser();
        app.setId(9L);
        app.setUserType(UserTypeEnum.OPEN_APP.getValue());
        app.setTenantId(1L);
        app.setInfo(Map.of("appKey", "k1"));

        assertThat(filterAndGetLoginUser("/open-api/member/user/get", app)).isNotNull();
        assertThat(filterAndGetLoginUser("/admin-api/system/user/page", app)).isNull();
        assertThat(filterAndGetLoginUser("/app-api/member/user/get", app)).isNull();
        assertThat(filterAndGetLoginUser("/rpc-api/system/tenant/valid", app)).isNull();
        assertThat(filterAndGetLoginUser("/actuator/health", app)).isNull();
    }

    @Test
    @DisplayName("/open-api 路径不接受管理员与会员身份")
    void open_api路径不接受管理员与会员身份() throws Exception {
        LoginUser admin = new LoginUser();
        admin.setId(1L);
        admin.setUserType(UserTypeEnum.ADMIN.getValue());
        admin.setTenantId(1L);
        LoginUser member = new LoginUser();
        member.setId(1L);
        member.setUserType(UserTypeEnum.MEMBER.getValue());
        member.setTenantId(1L);

        assertThat(filterAndGetLoginUser("/open-api/member/user/get", admin)).isNull();
        assertThat(filterAndGetLoginUser("/open-api/member/user/get", member)).isNull();
    }

    @Test
    @DisplayName("请求处理结束后必须清理上下文，避免容器线程复用时身份残留")
    void contextMustBeClearedAfterRequest() throws Exception {
        long ts = System.currentTimeMillis();
        String payload = encode(newLoginUser());
        run(request(payload, InternalSignUtils.sign(payload, ts, SECRET), ts));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(SecurityFrameworkUtils.getLoginUser()).isNull();
    }
}
