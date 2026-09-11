package com.ccloud.framework.rpc.core.interceptor;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.security.config.SecurityProperties;
import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.security.core.filter.LoginUserFilter;
import com.ccloud.framework.security.core.util.InternalSignUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import feign.RequestTemplate;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class InternalRequestInterceptorTest {

    private static final String SECRET = "ccloud-internal-sign-secret-for-test";

    private InternalRequestInterceptor interceptor;
    private SecurityProperties securityProperties;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        securityProperties.setInternalSignSecret(SECRET);
        interceptor = new InternalRequestInterceptor(securityProperties);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    private void login(Long userId, Long tenantId) {
        LoginUser user = new LoginUser();
        user.setId(userId);
        user.setUserType(UserTypeEnum.ADMIN.getValue());
        user.setTenantId(tenantId);
        user.setClientId("ccloud-web");
        user.setScopes(List.of("user.read"));
        user.setExpiresTime(LocalDateTime.now().plusHours(1));
        SecurityFrameworkUtils.setLoginUser(user, null);
    }

    private static String first(RequestTemplate template, String header) {
        Collection<String> values = template.headers().get(header);
        return values == null || values.isEmpty() ? null : values.iterator().next();
    }

    @Test
    @DisplayName("有登录用户时应写入身份、签名、时间戳与租户四个内部头")
    void shouldWriteAllFourInternalHeaders() {
        login(100L, 1L);
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(first(template, LoginUserFilter.HEADER_LOGIN_USER)).isNotBlank();
        assertThat(first(template, LoginUserFilter.HEADER_LOGIN_USER_SIGN)).isNotBlank();
        assertThat(first(template, LoginUserFilter.HEADER_TIMESTAMP)).isNotBlank();
        assertThat(first(template, InternalRequestInterceptor.HEADER_TENANT_ID)).isEqualTo("1");
    }

    @Test
    @DisplayName("生成的签名必须能被下游的 LoginUserFilter 验通过——两侧算法不一致会在此断掉")
    void generatedHeaders_mustBeAcceptedByDownstreamFilter() throws Exception {
        login(100L, 1L);
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        // 把 Feign 生成的头原样搬到一个下游请求上
        MockHttpServletRequest downstream = new MockHttpServletRequest("GET", "/rpc-api/system/user/get");
        downstream.addHeader(LoginUserFilter.HEADER_LOGIN_USER, first(template, LoginUserFilter.HEADER_LOGIN_USER));
        downstream.addHeader(LoginUserFilter.HEADER_LOGIN_USER_SIGN,
                first(template, LoginUserFilter.HEADER_LOGIN_USER_SIGN));
        downstream.addHeader(LoginUserFilter.HEADER_TIMESTAMP, first(template, LoginUserFilter.HEADER_TIMESTAMP));

        SecurityContextHolder.clearContext();
        AtomicReference<LoginUser> received = new AtomicReference<>();
        FilterChain chain = (req, resp) -> received.set(SecurityFrameworkUtils.getLoginUser());
        new LoginUserFilter(securityProperties).doFilter(downstream, new MockHttpServletResponse(), chain);

        assertThat(received.get()).isNotNull();
        assertThat(received.get().getId()).isEqualTo(100L);
        assertThat(received.get().getUserType()).isEqualTo(UserTypeEnum.ADMIN.getValue());
        assertThat(received.get().getTenantId()).isEqualTo(1L);
        assertThat(received.get().getScopes()).containsExactly("user.read");
    }

    @Test
    @DisplayName("签名可用同一密钥独立验证")
    void signatureShouldBeVerifiableWithSameSecret() {
        login(100L, 1L);
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        String payload = first(template, LoginUserFilter.HEADER_LOGIN_USER);
        long timestamp = Long.parseLong(first(template, LoginUserFilter.HEADER_TIMESTAMP));
        String sign = first(template, LoginUserFilter.HEADER_LOGIN_USER_SIGN);

        assertThat(InternalSignUtils.verify(payload, timestamp, sign, SECRET, java.time.Duration.ofMinutes(5)))
                .isTrue();
    }

    @Test
    @DisplayName("无登录用户但有租户上下文时，只透传租户头")
    void noLoginUser_shouldOnlyPropagateTenant() {
        TenantContextHolder.setTenantId(9L);
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(first(template, LoginUserFilter.HEADER_LOGIN_USER)).isNull();
        assertThat(first(template, InternalRequestInterceptor.HEADER_TENANT_ID)).isEqualTo("9");
    }

    @Test
    @DisplayName("既无用户也无租户时不应写入任何内部头")
    void noContext_shouldWriteNoHeaders() {
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(first(template, LoginUserFilter.HEADER_LOGIN_USER)).isNull();
        assertThat(first(template, InternalRequestInterceptor.HEADER_TENANT_ID)).isNull();
    }

    @Test
    @DisplayName("未配置签名密钥时不写身份头，避免下游收到一个永远验不过的头")
    void missingSecret_shouldNotWriteIdentityHeaders() {
        SecurityProperties noSecret = new SecurityProperties();
        login(100L, 1L);
        RequestTemplate template = new RequestTemplate();

        new InternalRequestInterceptor(noSecret).apply(template);

        assertThat(first(template, LoginUserFilter.HEADER_LOGIN_USER)).isNull();
    }

    @Test
    @DisplayName("租户上下文优先于登录用户自带的租户，二者不一致时以上下文为准")
    void tenantContextShouldTakePrecedence() {
        login(100L, 1L);
        TenantContextHolder.setTenantId(2L);
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(first(template, InternalRequestInterceptor.HEADER_TENANT_ID)).isEqualTo("2");
    }
}
