package com.ccloud.framework.monitor.core.filter;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.util.monitor.TracerUtils;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 把 TraceId 写入 MDC 与响应头。
 *
 * <p>MDC 里的值供 logback 的 {@code %X{traceId}} 使用，让每行日志都带上链路标识；
 * 响应头里的值让前端报障时能直接给出 ID，不必靠时间点大海捞针。
 */
@RequiredArgsConstructor
public class TraceFilter extends OncePerRequestFilter {

    public static final String HEADER_TRACE_ID = "trace-id";

    /**
     * 可为 null：未引入链路追踪组件时退化为自行生成 ID，
     * 日志串联依然可用，只是没有跨服务的 span 信息
     */
    @Nullable
    private final Tracer tracer;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        TracerUtils.setTraceId(traceId);
        response.setHeader(HEADER_TRACE_ID, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            // 容器线程会被复用，不清理会让下一个请求串上本次的 traceId
            TracerUtils.removeTraceId();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        // 上游传来的优先，保证跨服务是同一条链路
        String upstream = request.getHeader(HEADER_TRACE_ID);
        if (StrUtil.isNotBlank(upstream)) {
            return upstream;
        }
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        return IdUtil.fastSimpleUUID();
    }
}
