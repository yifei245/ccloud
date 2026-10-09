package com.ccloud.framework.apilog.core.util;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.operatelog.core.util.DesensitizeUtils;
import com.ccloud.framework.web.core.util.WebFrameworkUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 访问日志与异常日志共用的参数整理：脱敏后序列化，截断到列宽
 */
@Slf4j
public final class ApiLogUtils {

    public static final int MAX_LENGTH = 8000;

    private ApiLogUtils() {
    }

    /**
     * 取本次请求命中的处理方法；请求未命中 Controller（如 404）时为 null。
     * 异常处理器执行期间该 request attribute 仍在
     */
    public static HandlerMethod getHandlerMethod(HttpServletRequest request) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        return handler instanceof HandlerMethod method ? method : null;
    }

    /**
     * 方法上的 {@code @ApiAccessLog} 优先，其次类上的
     */
    public static ApiAccessLog getAnnotation(HandlerMethod handler) {
        ApiAccessLog annotation = handler.getMethodAnnotation(ApiAccessLog.class);
        return annotation != null ? annotation : handler.getBeanType().getAnnotation(ApiAccessLog.class);
    }

    /**
     * @return 本次请求对应的 {@code @ApiAccessLog}，没有时为 null
     */
    public static ApiAccessLog resolveAnnotation(HttpServletRequest request) {
        HandlerMethod handler = getHandlerMethod(request);
        return handler == null ? null : getAnnotation(handler);
    }

    /**
     * 租户编号请求头可能是任何客户端随手填的值：格式不对时当作没有，不能因此丢掉整条日志
     */
    public static Long getTenantId(HttpServletRequest request) {
        try {
            return WebFrameworkUtils.getTenantId(request);
        } catch (NumberFormatException e) {
            log.warn("[getTenantId] 租户编号请求头格式不正确，按无租户记录，value={}",
                    StrUtil.maxLength(request.getHeader(WebFrameworkUtils.HEADER_TENANT_ID), 50));
            return null;
        }
    }

    /**
     * @return {"query":{...},"body":{...}}，敏感字段为 ******；body 不是合法 JSON 时记为占位说明
     */
    public static String buildRequestParams(Map<String, String> query, String body, Collection<String> extraKeys) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("query", DesensitizeUtils.desensitize(query, extraKeys));
        if (body != null) {
            params.put("body", parseBody(body, extraKeys));
        }
        return StrUtil.maxLength(JsonUtils.toJsonString(params), MAX_LENGTH);
    }

    public static String buildResponseBody(Object result, Collection<String> extraKeys) {
        return result == null ? null
                : StrUtil.maxLength(JsonUtils.toJsonString(DesensitizeUtils.desensitize(result, extraKeys)), MAX_LENGTH);
    }

    /**
     * 解析不了的请求体不能原样落库：无法判断其中哪些是密码
     */
    private static Object parseBody(String body, Collection<String> extraKeys) {
        if (StrUtil.isBlank(body)) {
            return null;
        }
        try {
            return DesensitizeUtils.desensitize(JsonUtils.getObjectMapper().readTree(body), extraKeys);
        } catch (JsonProcessingException e) {
            return "<无法解析的请求体，长度 " + body.length() + ">";
        }
    }
}
