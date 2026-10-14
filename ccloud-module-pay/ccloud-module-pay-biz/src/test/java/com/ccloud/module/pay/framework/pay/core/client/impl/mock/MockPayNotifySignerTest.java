package com.ccloud.module.pay.framework.pay.core.client.impl.mock;

import com.ccloud.module.pay.framework.pay.core.client.PayNotifyVerifyException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockPayNotifySignerTest {

    private static final String SECRET = "0123456789abcdef";

    @Test
    void 正确签名通过() {
        long now = System.currentTimeMillis();
        String body = "{\"no\":\"P1\"}";
        String sign = MockPayNotifySigner.sign(SECRET, now, body);
        assertThatCode(() -> MockPayNotifySigner.verify(SECRET, headers(now, sign), body, now)).doesNotThrowAnyException();
    }

    @Test
    void 篡改报文_错误密钥_时间戳偏差超过5分钟_缺少签名头都拒绝() {
        long now = System.currentTimeMillis();
        String body = "{\"no\":\"P1\"}";
        String sign = MockPayNotifySigner.sign(SECRET, now, body);
        assertThatThrownBy(() -> MockPayNotifySigner.verify(SECRET, headers(now, sign), "{\"no\":\"P2\"}", now))
                .isInstanceOf(PayNotifyVerifyException.class);
        assertThatThrownBy(() -> MockPayNotifySigner.verify("another-secret-16", headers(now, sign), body, now))
                .isInstanceOf(PayNotifyVerifyException.class);
        long old = now - Duration.ofMinutes(6).toMillis();
        assertThatThrownBy(() -> MockPayNotifySigner.verify(SECRET, headers(old, MockPayNotifySigner.sign(SECRET, old, body)), body, now))
                .isInstanceOf(PayNotifyVerifyException.class);
        assertThatThrownBy(() -> MockPayNotifySigner.verify(SECRET, Map.of(), body, now))
                .isInstanceOf(PayNotifyVerifyException.class);
    }

    private static Map<String, String> headers(long ts, String sign) {
        return Map.of("x-mock-timestamp", String.valueOf(ts), "x-mock-sign", sign);
    }
}
