package com.ccloud.framework.common.util.servlet;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.net.multipart.UploadFile;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.util.json.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet 工具
 */
@Slf4j
public class ServletUtils {

    /**
     * 返回 JSON 字符串
     */
    public static void writeJSON(HttpServletResponse response, Object object) {
        String content = JsonUtils.toJsonString(object);
        try {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(content);
        } catch (IOException e) {
            log.error("[writeJSON] 响应写出失败", e);
        }
    }

    /**
     * 返回附件下载
     */
    public static void writeAttachment(HttpServletResponse response, String filename, byte[] content)
            throws IOException {
        response.setHeader("Content-Disposition", "attachment;filename="
                + URLEncoder.encode(filename, StandardCharsets.UTF_8));
        response.setContentType("application/octet-stream;charset=UTF-8");
        response.setContentLength(content.length);
        IoUtil.write(response.getOutputStream(), false, content);
    }

    public static String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "";
    }

    /**
     * 获取客户端真实 IP。网关与反向代理会层层追加 X-Forwarded-For，取第一个非 unknown 的值。
     */
    public static String getClientIP(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                int index = ip.indexOf(',');
                return index > 0 ? ip.substring(0, index).trim() : ip.trim();
            }
        }
        return request.getRemoteAddr();
    }

    public static boolean isJsonRequest(jakarta.servlet.ServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && StrUtil.startWithIgnoreCase(contentType, "application/json");
    }

    /**
     * 读取请求体。要求请求已被 CacheRequestBodyFilter 包装，否则只能读一次。
     */
    public static String getBody(HttpServletRequest request) {
        if (!isJsonRequest(request)) {
            return null;
        }
        try {
            return IoUtil.read(request.getReader());
        } catch (IOException e) {
            log.warn("[getBody] 读取请求体失败", e);
            return null;
        }
    }

    public static Map<String, String> getParamMap(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            result.put(name, request.getParameter(name));
        }
        return result;
    }

    public static Map<String, String> getHeaderMap(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return result;
        }
        for (String name : Collections.list(names)) {
            result.put(name, request.getHeader(name));
        }
        return result;
    }
}
