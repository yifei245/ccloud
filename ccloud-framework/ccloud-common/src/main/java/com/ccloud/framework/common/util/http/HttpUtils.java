package com.ccloud.framework.common.util.http;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * HTTP 工具
 */
public class HttpUtils {

    /**
     * 往 URL 上追加查询参数，已存在的键会被覆盖
     */
    public static String replaceUrlQuery(String url, String key, String value) {
        UrlBuilder builder = UrlBuilder.of(url, Charset.defaultCharset());
        Map<CharSequence, CharSequence> query = builder.getQuery() == null
                ? null : builder.getQuery().getQueryMap();
        if (query != null) {
            query.remove(key);
        }
        builder.addQuery(key, value);
        return builder.build();
    }

    /**
     * 把参数按键名字典序拼成 {@code k1=v1&k2=v2} 形式。
     * 开放 API 的签名串就基于该规则生成，务必与文档保持一致。
     */
    public static String buildSortedQueryString(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        Map<String, ?> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        sorted.forEach((k, v) -> {
            if (v == null || StrUtil.isBlank(String.valueOf(v))) {
                return;
            }
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(k).append('=').append(v);
        });
        return sb.toString();
    }

    public static String getUtf8String(byte[] bytes) {
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 判断是否为合法的重定向地址，防开放重定向。
     *
     * <p>必须与登记的地址<b>完全一致</b>（OAuth 2.1 要求）：前缀匹配可被
     * "https://app.example.com.evil.com" 之类的地址绕过，授权码会被发往攻击者控制的域名。
     *
     * @param url         待校验地址
     * @param allowedUris 登记的地址列表
     */
    public static boolean isRedirectUriValid(String url, java.util.Collection<String> allowedUris) {
        if (StringUtils.isBlank(url) || allowedUris == null || allowedUris.isEmpty()) {
            return false;
        }
        return allowedUris.stream().anyMatch(url::equals);
    }
}
