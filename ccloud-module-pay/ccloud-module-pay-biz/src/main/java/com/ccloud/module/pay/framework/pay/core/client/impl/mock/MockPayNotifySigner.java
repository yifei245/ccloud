package com.ccloud.module.pay.framework.pay.core.client.impl.mock;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.ccloud.module.pay.framework.pay.core.client.PayNotifyVerifyException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;

/**
 * Mock 回调签名：Base64(HMAC-SHA256(secret, timestamp + "\n" + body))
 */
public final class MockPayNotifySigner {

    public static final String HEADER_TIMESTAMP = "X-Mock-Timestamp";
    public static final String HEADER_SIGN = "X-Mock-Sign";
    private static final long MAX_SKEW_MILLIS = Duration.ofMinutes(5).toMillis();

    private MockPayNotifySigner() {
    }

    public static String sign(String secret, long timestamp, String body) {
        HMac mac = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8));
        return Base64.encode(mac.digest(timestamp + "\n" + body));
    }

    /**
     * @param headers 键为小写
     */
    public static void verify(String secret, Map<String, String> headers, String body, long now) {
        String ts = headers.get(HEADER_TIMESTAMP.toLowerCase());
        String sign = headers.get(HEADER_SIGN.toLowerCase());
        if (!NumberUtil.isLong(ts) || StrUtil.isBlank(sign)) {
            throw new PayNotifyVerifyException("缺少签名头");
        }
        long timestamp = Long.parseLong(ts);
        if (Math.abs(now - timestamp) > MAX_SKEW_MILLIS) {
            throw new PayNotifyVerifyException("时间戳超出允许范围");
        }
        byte[] expected = sign(secret, timestamp, StrUtil.nullToEmpty(body)).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, sign.getBytes(StandardCharsets.UTF_8))) {
            throw new PayNotifyVerifyException("签名不正确");
        }
    }
}
