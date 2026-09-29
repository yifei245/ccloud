package com.ccloud.framework.common.util.http;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HttpUtilsRedirectUriTest {

    private static final List<String> ALLOWED = List.of("http://localhost:8080/cb", "https://app.ccloud.local");

    @Test
    void 与登记地址完全一致时合法() {
        assertThat(HttpUtils.isRedirectUriValid("http://localhost:8080/cb", ALLOWED)).isTrue();
        assertThat(HttpUtils.isRedirectUriValid("https://app.ccloud.local", ALLOWED)).isTrue();
    }

    /**
     * 前缀匹配可被 "https://app.ccloud.local.evil.com" 这类地址绕过，授权码会被发往攻击者控制的域名
     */
    @Test
    void 前缀相同但不完全一致时不合法() {
        assertThat(HttpUtils.isRedirectUriValid("https://app.ccloud.local.evil.com", ALLOWED)).isFalse();
        assertThat(HttpUtils.isRedirectUriValid("https://app.ccloud.local@evil.com", ALLOWED)).isFalse();
        assertThat(HttpUtils.isRedirectUriValid("http://localhost:8080/cb/../steal", ALLOWED)).isFalse();
        assertThat(HttpUtils.isRedirectUriValid("http://localhost:8080/cb?x=1", ALLOWED)).isFalse();
    }

    @Test
    void 空值不合法() {
        assertThat(HttpUtils.isRedirectUriValid("", ALLOWED)).isFalse();
        assertThat(HttpUtils.isRedirectUriValid("http://localhost:8080/cb", List.of())).isFalse();
        assertThat(HttpUtils.isRedirectUriValid("http://localhost:8080/cb", null)).isFalse();
    }
}
