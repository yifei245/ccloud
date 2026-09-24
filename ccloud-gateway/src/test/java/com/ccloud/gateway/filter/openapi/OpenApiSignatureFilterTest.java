package com.ccloud.gateway.filter.openapi;

import com.ccloud.gateway.support.GatewayIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 开放接口签名：{@code Base64(HmacSHA256(appSecret, METHOD\nPATH\nQUERY\nTIMESTAMP\nNONCE\nSHA256_HEX(BODY)))}
 */
class OpenApiSignatureFilterTest extends GatewayIntegrationTestSupport {

    private static final String PATH = "/open-api/pay/order/create";
    private static final String BODY = "{\"amount\":100}";

    @Test
    void open_api路径签名正确应放行_租户取自应用_请求体完整转发() {
        signedPost(PATH, BODY).exchange().expectStatus().isOk();

        assertThat(PAY.requests).hasSize(1);
        assertThat(PAY.requests.get(0).body()).isEqualTo(BODY);
        assertThat(PAY.requests.get(0).headers().get("X-Tenant-Id")).containsExactly(String.valueOf(OPEN_APP_TENANT_ID));
        assertThat(PAY.requests.get(0).headers().getFirst("X-Open-App-Id")).isEqualTo(OPEN_APP_ID);
    }

    @Test
    void open_api路径签名错误应返回401() {
        post(PATH, BODY, System.currentTimeMillis(), UUID.randomUUID().toString(), "wrong-sign", OPEN_APP_ID)
                .exchange().expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.code").isEqualTo(401);

        assertThat(PAY.requests).isEmpty();
    }

    @Test
    void open_api路径nonce重复应返回401() {
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();
        String sign = sign(timestamp, nonce, BODY);

        post(PATH, BODY, timestamp, nonce, sign, OPEN_APP_ID).exchange().expectStatus().isOk();
        post(PATH, BODY, timestamp, nonce, sign, OPEN_APP_ID).exchange().expectStatus().isUnauthorized();

        assertThat(PAY.requests).hasSize(1);
    }

    @Test
    void 请求体被篡改应返回401() {
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();

        post(PATH, "{\"amount\":1}", timestamp, nonce, sign(timestamp, nonce, BODY), OPEN_APP_ID)
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void 时间戳超出5分钟应返回401_nonce只需保存有限时长() {
        long timestamp = System.currentTimeMillis() - 6 * 60 * 1000;
        String nonce = UUID.randomUUID().toString();

        post(PATH, BODY, timestamp, nonce, sign(timestamp, nonce, BODY), OPEN_APP_ID)
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void 未知或已停用的应用应返回401() {
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();
        post(PATH, BODY, timestamp, nonce, sign(timestamp, nonce, BODY), "unknown-app")
                .exchange().expectStatus().isUnauthorized();

        String disabledSign = openApiSign("disabled-secret", "POST", PATH, null, timestamp, nonce,
                BODY.getBytes(StandardCharsets.UTF_8));
        post(PATH, BODY, timestamp, UUID.randomUUID().toString(), disabledSign, "disabled-app")
                .exchange().expectStatus().isUnauthorized();
        assertThat(PAY.requests).isEmpty();
    }

    @Test
    void 缺少签名头应返回401() {
        webClient.post().uri(PATH).contentType(MediaType.APPLICATION_JSON).bodyValue(BODY)
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void 非open_api路径不走签名校验() {
        webClient.post().uri("/admin-api/system/auth/login").exchange().expectStatus().isOk();
        assertThat(SYSTEM.requests).hasSize(1);
    }

    private String sign(long timestamp, String nonce, String body) {
        return openApiSign(OPEN_APP_SECRET, "POST", PATH, null, timestamp, nonce, body.getBytes(StandardCharsets.UTF_8));
    }

    private org.springframework.test.web.reactive.server.WebTestClient.RequestHeadersSpec<?> post(
            String path, String body, long timestamp, String nonce, String sign, String appId) {
        return webClient.post().uri(path)
                .header("X-Open-App-Id", appId)
                .header("X-Open-Timestamp", String.valueOf(timestamp))
                .header("X-Open-Nonce", nonce)
                .header("X-Open-Sign", sign)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }
}
