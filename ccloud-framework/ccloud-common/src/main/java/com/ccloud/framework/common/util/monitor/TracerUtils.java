package com.ccloud.framework.common.util.monitor;

import org.slf4j.MDC;

/**
 * 链路追踪工具。
 *
 * <p>放在 common 而非 monitor starter，是为了让不引入追踪组件的模块
 * （例如纯工具模块）也能读到 TraceId 用于日志。真正写入 MDC 的是 monitor starter。
 */
public class TracerUtils {

    /**
     * MDC 中 TraceId 的键名，与 logback pattern 里的 %X{traceId} 对应
     */
    public static final String TRACE_ID = "traceId";

    public static String getTraceId() {
        String traceId = MDC.get(TRACE_ID);
        return traceId == null ? "" : traceId;
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static void removeTraceId() {
        MDC.remove(TRACE_ID);
    }
}
