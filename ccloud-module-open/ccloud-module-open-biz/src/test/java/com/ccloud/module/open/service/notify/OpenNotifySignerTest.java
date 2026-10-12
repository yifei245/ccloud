package com.ccloud.module.open.service.notify;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class OpenNotifySignerTest {

    @Test
    void 签名与独立实现一致() throws Exception {
        String body = "{\"eventId\":\"e1\"}";
        String expected = Base64.getEncoder().encodeToString(hmac("secret",
                "1700000000000\ne1\n" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(body.getBytes(UTF_8)))));
        assertThat(OpenNotifySigner.sign("secret", 1700000000000L, "e1", body)).isEqualTo(expected);
    }

    private static byte[] hmac(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256"));
        return mac.doFinal(data.getBytes(UTF_8));
    }
}
