package com.ccloud.gateway.filter.security;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * 匿名接口匹配。条目为 Ant 风格路径，可带方法前缀：{@code GET /admin-api/system/sso/authorize}
 */
public class IgnoreUrlsMatcher {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<Entry> entries;

    private record Entry(HttpMethod method, String pattern) {
    }

    public IgnoreUrlsMatcher(List<String> ignoreUrls) {
        this.entries = ignoreUrls.stream().map(IgnoreUrlsMatcher::parse).toList();
    }

    private static Entry parse(String value) {
        String trimmed = value.trim();
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            return new Entry(null, trimmed);
        }
        return new Entry(HttpMethod.valueOf(trimmed.substring(0, space).toUpperCase()),
                trimmed.substring(space + 1).trim());
    }

    public boolean matches(HttpMethod method, String path) {
        return entries.stream().anyMatch(entry -> (entry.method() == null || entry.method().equals(method))
                && pathMatcher.match(entry.pattern(), path));
    }
}
