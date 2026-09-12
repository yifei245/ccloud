package com.ccloud.framework.signature.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 开放 API 的签名校验。第三方系统凭它证明"请求确实来自持有 appSecret 的一方"，
 * 是 /open-api 这一族接口的唯一身份依据。
 */
class ApiSignatureVerifierTest {

    private static final String APP_SECRET = "demo-app-secret";

    private ApiSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new ApiSignatureVerifier(Duration.ofMinutes(5));
    }

    private String sign(Map<String, String> params, String body, long timestamp, String nonce, String secret) {
        return verifier.sign(params, body, timestamp, nonce, secret);
    }

    @Test
    @DisplayName("正确签名应校验通过")
    void correctSignature_shouldPass() {
        long ts = System.currentTimeMillis();
        Map<String, String> params = Map.of("pageNo", "1", "pageSize", "10");
        String sign = sign(params, "{\"a\":1}", ts, "nonce-1", APP_SECRET);

        assertThat(verifier.verify(params, "{\"a\":1}", ts, "nonce-1", sign, APP_SECRET)).isTrue();
    }

    @Test
    @DisplayName("参数顺序不同但内容相同时签名应一致——调用方无法保证传参顺序")
    void signature_shouldBeOrderIndependent() {
        long ts = System.currentTimeMillis();
        String a = sign(new java.util.LinkedHashMap<>(Map.of("b", "2", "a", "1")), "", ts, "n", APP_SECRET);
        String b = sign(new java.util.TreeMap<>(Map.of("a", "1", "b", "2")), "", ts, "n", APP_SECRET);
        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("篡改任一查询参数后签名失效")
    void tamperedParam_shouldFail() {
        long ts = System.currentTimeMillis();
        String sign = sign(Map.of("amount", "100"), "", ts, "n", APP_SECRET);
        assertThat(verifier.verify(Map.of("amount", "999"), "", ts, "n", sign, APP_SECRET)).isFalse();
    }

    @Test
    @DisplayName("篡改请求体后签名失效——签名必须覆盖 body，否则改金额无人能察觉")
    void tamperedBody_shouldFail() {
        long ts = System.currentTimeMillis();
        String sign = sign(Map.of(), "{\"amount\":100}", ts, "n", APP_SECRET);
        assertThat(verifier.verify(Map.of(), "{\"amount\":999}", ts, "n", sign, APP_SECRET)).isFalse();
    }

    @Test
    @DisplayName("换用他人 secret 签名应失败")
    void wrongSecret_shouldFail() {
        long ts = System.currentTimeMillis();
        String sign = sign(Map.of(), "", ts, "n", APP_SECRET);
        assertThat(verifier.verify(Map.of(), "", ts, "n", sign, "attacker-secret")).isFalse();
    }

    @Test
    @DisplayName("时间戳超出容差应失败，限制抓包重放的时间窗")
    void staleTimestamp_shouldFail() {
        long stale = System.currentTimeMillis() - Duration.ofMinutes(6).toMillis();
        String sign = sign(Map.of(), "", stale, "n", APP_SECRET);
        assertThat(verifier.verify(Map.of(), "", stale, "n", sign, APP_SECRET)).isFalse();
    }

    @Test
    @DisplayName("nonce 参与签名，改 nonce 即失效")
    void tamperedNonce_shouldFail() {
        long ts = System.currentTimeMillis();
        String sign = sign(Map.of(), "", ts, "n1", APP_SECRET);
        assertThat(verifier.verify(Map.of(), "", ts, "n2", sign, APP_SECRET)).isFalse();
    }

    @Test
    @DisplayName("空值参数不参与签名，避免调用方对空字段的处理差异导致验签失败")
    void blankParams_shouldBeExcluded() {
        long ts = System.currentTimeMillis();
        java.util.Map<String, String> withBlank = new java.util.HashMap<>();
        withBlank.put("a", "1");
        withBlank.put("b", "");
        assertThat(sign(withBlank, "", ts, "n", APP_SECRET))
                .isEqualTo(sign(Map.of("a", "1"), "", ts, "n", APP_SECRET));
    }

    @Test
    @DisplayName("签名为空或格式非法时判为失败而非抛异常")
    void malformedSignature_shouldReturnFalse() {
        long ts = System.currentTimeMillis();
        assertThat(verifier.verify(Map.of(), "", ts, "n", null, APP_SECRET)).isFalse();
        assertThat(verifier.verify(Map.of(), "", ts, "n", "", APP_SECRET)).isFalse();
        assertThat(verifier.verify(Map.of(), "", ts, "n", "!!!", APP_SECRET)).isFalse();
    }
}
