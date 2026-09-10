package com.ccloud.framework.tenant.core.web;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.framework.tenant.config.TenantProperties;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.tenant.core.service.TenantFrameworkService;
import com.ccloud.framework.web.config.WebProperties;
import com.ccloud.framework.web.core.filter.ApiRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.PathMatcher;

import java.io.IOException;
import java.util.Objects;

/**
 * 租户安全校验。运行在 Spring Security 之后，此时已能拿到登录身份。
 *
 * <p>拦住两件事：
 * <ol>
 *   <li>登录用户所属租户与请求声明的租户不一致——这是跨租户越权的主要入口，
 *       攻击者只需改一个请求头；</li>
 *   <li>租户本身不可用（不存在 / 已禁用 / 已过期）。</li>
 * </ol>
 */
@Slf4j
public class TenantSecurityWebFilter extends ApiRequestFilter {

    /**
     * 登录用户所属租户，由 security starter 写入 request attribute
     */
    public static final String REQUEST_ATTRIBUTE_LOGIN_USER_TENANT_ID = "ccloud.login_user_tenant_id";

    private final TenantProperties tenantProperties;
    private final PathMatcher pathMatcher;
    private final TenantFrameworkService tenantFrameworkService;

    public TenantSecurityWebFilter(TenantProperties tenantProperties, WebProperties webProperties,
                                   PathMatcher pathMatcher, TenantFrameworkService tenantFrameworkService) {
        super(webProperties);
        this.tenantProperties = tenantProperties;
        this.pathMatcher = pathMatcher;
        this.tenantFrameworkService = tenantFrameworkService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Long tenantId = TenantContextHolder.getTenantId();
        Long loginUserTenantId = (Long) request.getAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_TENANT_ID);

        // 已登录时，请求声明的租户必须与用户所属租户一致
        if (loginUserTenantId != null && !Objects.equals(loginUserTenantId, tenantId)) {
            log.warn("[doFilterInternal] 租户不匹配，登录用户租户 {}，请求租户 {}，url={}",
                    loginUserTenantId, tenantId, request.getRequestURI());
            ServletUtils.writeJSON(response, CommonResult.error(GlobalErrorCodeConstants.FORBIDDEN.getCode(),
                    "您无权访问该租户的数据"));
            return;
        }

        if (tenantId != null && !isIgnoreUrl(request)) {
            try {
                tenantFrameworkService.validTenant(tenantId);
            } catch (ServiceException ex) {
                ServletUtils.writeJSON(response, CommonResult.error(ex.getCode(), ex.getMessage()));
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return super.shouldNotFilter(request) || isIgnoreUrl(request);
    }

    private boolean isIgnoreUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return tenantProperties.getIgnoreUrls().stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }
}
