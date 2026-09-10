package com.ccloud.framework.web.core.filter;

import com.ccloud.framework.web.config.WebProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 只对业务 API 生效的过滤器基类。静态资源、actuator 等不必要的路径直接跳过，
 * 避免无谓开销与噪声日志。
 */
@RequiredArgsConstructor
public abstract class ApiRequestFilter extends OncePerRequestFilter {

    protected final WebProperties webProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !(uri.startsWith(webProperties.getAdminApi().getPrefix())
                || uri.startsWith(webProperties.getAppApi().getPrefix())
                || uri.startsWith(webProperties.getOpenApi().getPrefix()));
    }
}
