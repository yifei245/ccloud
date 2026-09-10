package com.ccloud.framework.xss.core;

import com.ccloud.framework.xss.config.XssProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * XSS 过滤器，清洗 query 参数与 header
 */
@RequiredArgsConstructor
public class XssFilter extends OncePerRequestFilter {

    private final XssProperties properties;
    private final PathMatcher pathMatcher;
    private final XssCleaner xssCleaner;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(new XssRequestWrapper(request, xssCleaner), response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnable()) {
            return true;
        }
        String uri = request.getRequestURI();
        return properties.getExcludeUrls().stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }
}
