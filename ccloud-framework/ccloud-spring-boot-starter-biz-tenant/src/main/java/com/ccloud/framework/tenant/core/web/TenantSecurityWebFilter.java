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
 *   <li>租户本身不可用（不存在 / 已禁用 / 已过期）；</li>
 *   <li>管理端、用户端接口未携带租户编号（白名单路径除外）。</li>
 * </ol>
 */
@Slf4j
public class TenantSecurityWebFilter extends ApiRequestFilter {

    /**
     * 登录用户所属租户，由 security starter 写入 request attribute
     */
    public static final String REQUEST_ATTRIBUTE_LOGIN_USER_TENANT_ID = "ccloud.login_user_tenant_id";

    private final TenantProperties tenantProperties;
    private final String openApiPrefix;
    private final PathMatcher pathMatcher;
    private final TenantFrameworkService tenantFrameworkService;

    public TenantSecurityWebFilter(TenantProperties tenantProperties, WebProperties webProperties,
                                   PathMatcher pathMatcher, TenantFrameworkService tenantFrameworkService) {
        super(webProperties);
        this.openApiPrefix = webProperties.getOpenApi().getPrefix();
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

        // 未携带租户编号时持久层不做租户过滤，匿名接口（如登录）会跨全部租户查询，必须拒绝。
        // 开放接口由开放平台按应用确定租户，不在此要求
        if (tenantId == null && !isOpenApi(request)) {
            log.warn("[doFilterInternal] 未携带租户编号，url={}", request.getRequestURI());
            ServletUtils.writeJSON(response, CommonResult.error(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
                    "请求未携带租户编号"));
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

    private boolean isOpenApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(openApiPrefix);
    }

    private boolean isIgnoreUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return tenantProperties.getIgnoreUrls().stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }
}
