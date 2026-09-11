package com.ccloud.framework.security.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 内部身份头验签。这是"业务服务凭什么相信请求里的身份"的全部依据——
 * 一旦它能被绕过，任何人都可以伪造一个 X-Login-User 头冒充管理员。
 */
class InternalSignUtilsTest {

    private static final String SECRET = "ccloud-internal-sign-secret-for-test";
    private static final Duration TOLERANCE = Duration.ofMinutes(5);

    @Test
    @DisplayName("签名与验签往返应通过")
    void signThenVerify_shouldPass() {
        long ts = System.currentTimeMillis();
        String sign = InternalSignUtils.sign("payload", ts, SECRET);
        assertThat(InternalSignUtils.verify("payload", ts, sign, SECRET, TOLERANCE)).isTrue();
    }

    @Test
    @DisplayName("篡改 payload 后验签必须失败")
    void verify_shouldFailWhenPayloadTampered() {
        long ts = System.currentTimeMillis();
        String sign = InternalSignUtils.sign("payload", ts, SECRET);
        assertThat(InternalSignUtils.verify("evil", ts, sign, SECRET, TOLERANCE)).isFalse();
    }

    @Test
    @DisplayName("篡改时间戳后验签必须失败，防止重放旧签名")
    void verify_shouldFailWhenTimestampTampered() {
        long ts = System.currentTimeMillis();
        String sign = InternalSignUtils.sign("payload", ts, SECRET);
        assertThat(InternalSignUtils.verify("payload", ts + 1, sign, SECRET, TOLERANCE)).isFalse();
    }

    @Test
    @DisplayName("超过时间容差后验签必须失败")
    void verify_shouldFailWhenOutsideTolerance() {
        long ts = System.currentTimeMillis() - Duration.ofMinutes(6).toMillis();
        String sign = InternalSignUtils.sign("payload", ts, SECRET);
        assertThat(InternalSignUtils.verify("payload", ts, sign, SECRET, TOLERANCE)).isFalse();
    }

    @Test
    @DisplayName("时钟略微超前的请求应被接受，避免多机时钟漂移导致误杀")
    void verify_shouldTolerateSmallClockSkewAhead() {
        long ts = System.currentTimeMillis() + Duration.ofSeconds(30).toMillis();
        String sign = InternalSignUtils.sign("payload", ts, SECRET);
        assertThat(InternalSignUtils.verify("payload", ts, sign, SECRET, TOLERANCE)).isTrue();
    }

    @Test
    @DisplayName("密钥不同验签必须失败")
    void verify_shouldFailWithDifferentSecret() {
        long ts = System.currentTimeMillis();
        String sign = InternalSignUtils.sign("payload", ts, SECRET);
        assertThat(InternalSignUtils.verify("payload", ts, sign, "another-secret", TOLERANCE)).isFalse();
    }

    @Test
    @DisplayName("签名为空或格式非法时应判为失败，而不是抛异常中断请求")
    void verify_shouldReturnFalseRatherThanThrowOnMalformedSign() {
        long ts = System.currentTimeMillis();
        assertThat(InternalSignUtils.verify("payload", ts, null, SECRET, TOLERANCE)).isFalse();
        assertThat(InternalSignUtils.verify("payload", ts, "", SECRET, TOLERANCE)).isFalse();
        assertThat(InternalSignUtils.verify("payload", ts, "!!!not-base64!!!", SECRET, TOLERANCE)).isFalse();
    }

    @Test
    @DisplayName("相同输入应产生相同签名，保证网关与业务侧算法一致")
    void sign_shouldBeDeterministic() {
        long ts = 1_700_000_000_000L;
        assertThat(InternalSignUtils.sign("payload", ts, SECRET))
                .isEqualTo(InternalSignUtils.sign("payload", ts, SECRET));
    }
}
