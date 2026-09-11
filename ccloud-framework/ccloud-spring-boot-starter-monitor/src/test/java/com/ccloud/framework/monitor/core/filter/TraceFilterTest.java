package com.ccloud.framework.monitor.core.filter;

import com.ccloud.framework.common.util.monitor.TracerUtils;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceFilterTest {

    private final TraceFilter filter = new TraceFilter(null);

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("响应应携带非空的 trace-id 头，便于按 ID 检索整条链路的日志")
    void response_shouldCarryTraceIdHeader() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/admin-api/x"), response, new MockFilterChain());

        assertThat(response.getHeader(TraceFilter.HEADER_TRACE_ID)).isNotBlank();
    }

    @Test
    @DisplayName("请求处理期间 MDC 中应有 traceId，logback 才能把它打进每行日志")
    void mdc_shouldContainTraceIdDuringRequest() throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = (req, resp) -> captured.set(TracerUtils.getTraceId());

        filter.doFilter(new MockHttpServletRequest("GET", "/admin-api/x"),
                new MockHttpServletResponse(), chain);

        assertThat(captured.get()).isNotBlank();
    }

    @Test
    @DisplayName("请求结束后必须清理 MDC，否则容器线程复用时会串上一个请求的 traceId")
    void mdc_shouldBeClearedAfterRequest() throws Exception {
        filter.doFilter(new MockHttpServletRequest("GET", "/admin-api/x"),
                new MockHttpServletResponse(), new MockFilterChain());

        assertThat(MDC.get(TracerUtils.TRACE_ID)).isNull();
    }

    @Test
    @DisplayName("上游已带 trace-id 时应沿用，保证跨服务是同一条链路")
    void shouldReuseUpstreamTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-api/x");
        request.addHeader(TraceFilter.HEADER_TRACE_ID, "upstream-trace-123");
        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = (req, resp) -> captured.set(TracerUtils.getTraceId());

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(captured.get()).isEqualTo("upstream-trace-123");
    }

    @Test
    @DisplayName("链内抛异常时也要清理 MDC")
    void mdc_shouldBeClearedEvenOnException() {
        FilterChain chain = (req, resp) -> {
            throw new IllegalStateException("boom");
        };
        try {
            filter.doFilter(new MockHttpServletRequest("GET", "/admin-api/x"),
                    new MockHttpServletResponse(), chain);
        } catch (Exception ignored) {
            // 预期抛出
        }
        assertThat(MDC.get(TracerUtils.TRACE_ID)).isNull();
    }
}
