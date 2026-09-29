package com.ccloud.framework.security.core.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

/**
 * 内部身份头的签名与验签。
 *
 * <p>网关是唯一的身份签发方，业务服务凭签名判断"这个身份确实来自网关"。
 * 没有它，任何能直连业务服务端口的人都可以伪造一个 X-Login-User 头冒充任意用户。
 *
 * <p>签名串为 {@code payload + "." + timestamp}，带上时间戳是为了让旧签名过期，
 * 否则一次抓包就能永久重放。
 */
@Slf4j
public class InternalSignUtils {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * 允许的时钟超前量。多机部署时各节点时钟难免有几十秒的漂移，
     * 完全不容忍会导致随机性的鉴权失败。
     */
    private static final Duration CLOCK_SKEW_AHEAD = Duration.ofMinutes(1);

    public static String sign(String payload, long timestamp, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] raw = mac.doFinal((payload + "." + timestamp).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("内部身份签名失败", e);
        }
    }

    /**
     * 验签。任何异常都归为"验签失败"而非向上抛出——
     * 伪造的输入不应该让服务返回 500，那本身就是一种信息泄漏。
     *
     * @param tolerance 时间戳允许的滞后量
     */
    public static boolean verify(String payload, long timestamp, String sign, String secret, Duration tolerance) {
        if (sign == null || sign.isEmpty()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (timestamp < now - tolerance.toMillis() || timestamp > now + CLOCK_SKEW_AHEAD.toMillis()) {
            return false;
        }
        try {
            String expected = sign(payload, timestamp, secret);
            // 定长比较，避免通过响应时间差逐字节猜出签名
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    sign.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.debug("[verify] 验签异常，判为失败", e);
            return false;
        }
    }
}
