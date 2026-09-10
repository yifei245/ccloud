package com.ccloud.framework.tenant.core.web;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.tenant.config.TenantProperties;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.tenant.core.service.TenantFrameworkService;
import com.ccloud.framework.web.config.WebProperties;
import com.ccloud.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.AntPathMatcher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class TenantSecurityWebFilterTest {

    private TenantFrameworkService tenantFrameworkService;
    private TenantSecurityWebFilter filter;

    @BeforeEach
    void setUp() {
        TenantProperties properties = new TenantProperties();
        properties.setIgnoreUrls(List.of("/admin-api/system/auth/login"));
        tenantFrameworkService = mock(TenantFrameworkService.class);
        filter = new TenantSecurityWebFilter(properties, new WebProperties(),
                new AntPathMatcher(), tenantFrameworkService);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        return request;
    }

    @Test
    @DisplayName("登录用户的租户与请求头租户不一致时应拒绝，这是跨租户越权的主要入口")
    void shouldRejectWhenLoginTenantDiffersFromHeader() throws Exception {
        MockHttpServletRequest request = request("/admin-api/system/user/page");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebFrameworkUtils.setLoginUserId(request, 100L);
        request.setAttribute("ccloud.login_user_tenant_id", 2L);
        TenantContextHolder.setTenantId(1L);

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        CommonResult<?> result = JsonUtils.parseObject(response.getContentAsString(), CommonResult.class);
        assertThat(result.getCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("租户校验不通过时应中断请求并返回业务错误")
    void shouldRejectWhenTenantInvalid() throws Exception {
        doThrow(new com.ccloud.framework.common.exception.ServiceException(1_001_000_100, "租户已禁用"))
                .when(tenantFrameworkService).validTenant(anyLong());

        MockHttpServletRequest request = request("/admin-api/system/user/page");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TenantContextHolder.setTenantId(5L);

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        CommonResult<?> result = JsonUtils.parseObject(response.getContentAsString(), CommonResult.class);
        assertThat(result.getCode()).isEqualTo(1_001_000_100);
        assertThat(result.getMsg()).isEqualTo("租户已禁用");
    }

    @Test
    @DisplayName("白名单路径直接放行，且不做租户校验")
    void shouldPassThroughIgnoredUrls() throws Exception {
        MockHttpServletRequest request = request("/admin-api/system/auth/login");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(tenantFrameworkService, never()).validTenant(anyLong());
    }

    @Test
    @DisplayName("租户合法时应放行并完成校验")
    void shouldPassWhenTenantValid() throws Exception {
        MockHttpServletRequest request = request("/admin-api/system/user/page");
        TenantContextHolder.setTenantId(1L);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(tenantFrameworkService).validTenant(1L);
    }

    @Test
    @DisplayName("非 admin/app 前缀的路径不参与租户校验")
    void shouldSkipNonBusinessApi() throws Exception {
        MockHttpServletRequest request = request("/actuator/health");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(tenantFrameworkService, never()).validTenant(anyLong());
    }
}
