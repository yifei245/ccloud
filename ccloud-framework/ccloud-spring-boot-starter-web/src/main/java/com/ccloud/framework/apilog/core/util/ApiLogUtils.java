package com.ccloud.framework.apilog.core.util;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.operatelog.core.util.DesensitizeUtils;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 访问日志与异常日志共用的参数整理：脱敏后序列化，截断到列宽
 */
public final class ApiLogUtils {

    public static final int MAX_LENGTH = 8000;

    private ApiLogUtils() {
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
