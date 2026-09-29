package com.ccloud.framework.xss.core;

/**
 * XSS 清洗器
 */
public interface XssCleaner {

    /**
     * 清洗 HTML 中的危险内容
     *
     * @param html 原始内容，可能为 null
     * @return 清洗后的内容；入参为 null 时返回 null
     */
    String clean(String html);
}
