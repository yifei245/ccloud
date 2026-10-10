package com.ccloud.module.open.service.notify;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 回调签名：X-Open-Sign = Base64(HmacSHA256(appSecret, X-Open-Timestamp + "\n" + eventId + "\n" + 十六进制小写 SHA-256(请求体)))
 */
public final class OpenNotifySigner {

    private OpenNotifySigner() {
    }

    public static String sign(String secret, long timestamp, String eventId, String body) {
        try {
            String bodyHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8)));
            String stringToSign = timestamp + "\n" + eventId + "\n" + bodyHash;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("回调签名计算失败", e);
        }
    }
}
