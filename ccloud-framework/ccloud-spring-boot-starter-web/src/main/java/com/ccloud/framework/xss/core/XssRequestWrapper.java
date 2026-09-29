package com.ccloud.framework.xss.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * 对请求参数做 XSS 清洗的包装器。
 *
 * <p>只处理 query 参数与 header；JSON 请求体的清洗由 Jackson 反序列化器完成
 * （见 {@link XssStringJsonDeserializer}），因为在这里改 body 会破坏流的一致性。
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    private final XssCleaner xssCleaner;

    public XssRequestWrapper(HttpServletRequest request, XssCleaner xssCleaner) {
        super(request);
        this.xssCleaner = xssCleaner;
    }

    @Override
    public String getParameter(String name) {
        return xssCleaner.clean(super.getParameter(name));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] cleaned = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleaned[i] = xssCleaner.clean(values[i]);
        }
        return cleaned;
    }

    @Override
    public String getHeader(String name) {
        return xssCleaner.clean(super.getHeader(name));
    }
}
