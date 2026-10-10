package com.ccloud.gateway.filter.openapi;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.gateway.filter.security.LoginUser;
import com.ccloud.gateway.support.GatewayIntegrationTestSupport;
import com.ccloud.gateway.support.StubServer;
import com.sun.net.httpserver.Headers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 开放接口：应用由 open 服务管理，网关经 RPC 读取（测试中为 OPEN 桩）。校验顺序：
 * 应用存在 / 启用 / 未过期（401）→ IP 白名单（403）→ 时间戳、请求体大小、签名（401）→ nonce（401）→ 接口授权（403）。
 * 签名：{@code Base64(HmacSHA256(appSecret, METHOD\nPATH\nQUERY\nTIMESTAMP\nNONCE\nSHA256_HEX(BODY)))}
 */
class OpenApiSignatureFilterTest extends GatewayIntegrationTestSupport {

    private static final String PATH = "/open-api/pay/order/create";
    private static final String BODY = "{\"amount\":100}";

    @Autowired
    private ReactiveStringRedisTemplate stringRedisTemplate;

    @Test
    void 签名正确放行_下游收到开放应用身份头与租户_签名头被移除() throws Exception {
        registerDemoOpenApp();
        signedPost(PATH, BODY).exchange().expectStatus().isOk();

        assertThat(PAY.requests).hasSize(1);
        StubServer.Received r = PAY.requests.get(0);
        assertThat(r.headers().get("X-Tenant-Id")).containsExactly(String.valueOf(OPEN_APP_TENANT_ID));
        assertThat(r.headers().getFirst("X-Open-Sign")).isNull();
        LoginUser user = decodeLoginUser(r.headers());
        assertThat(user.getId()).isEqualTo(11L);
        assertThat(user.getUserType()).isEqualTo(UserTypeEnum.OPEN_APP.getValue());
        assertThat(user.getTenantId()).isEqualTo(OPEN_APP_TENANT_ID);
        assertThat(user.getInfo()).containsEntry("appKey", OPEN_APP_ID);
        assertThat(r.body()).isEqualTo(BODY);
    }

    @Test
    void open_api路径签名错误应返回401() {
        registerDemoOpenApp();
        post(PATH, BODY, System.currentTimeMillis(), UUID.randomUUID().toString(), "wrong-sign", OPEN_APP_ID)
                .exchange().expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.code").isEqualTo(401);

        assertThat(PAY.requests).isEmpty();
    }

    @Test
    void open_api路径nonce重复应返回401() {
        registerDemoOpenApp();
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();
        String sign = sign(timestamp, nonce, BODY);

        post(PATH, BODY, timestamp, nonce, sign, OPEN_APP_ID).exchange().expectStatus().isOk();
        post(PATH, BODY, timestamp, nonce, sign, OPEN_APP_ID).exchange().expectStatus().isUnauthorized();

        assertThat(PAY.requests).hasSize(1);
    }

    @Test
    void 请求体被篡改应返回401() {
        registerDemoOpenApp();
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();

        post(PATH, "{\"amount\":1}", timestamp, nonce, sign(timestamp, nonce, BODY), OPEN_APP_ID)
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void 时间戳超出5分钟应返回401_nonce只需保存有限时长() {
        registerDemoOpenApp();
        long timestamp = System.currentTimeMillis() - 6 * 60 * 1000;
        String nonce = UUID.randomUUID().toString();

        post(PATH, BODY, timestamp, nonce, sign(timestamp, nonce, BODY), OPEN_APP_ID)
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void 应用不存在_已停用_已过期返回401() {
        // 未登记：open 返回 data 为 null
        signedPost(PATH, "{}").exchange().expectStatus().isUnauthorized();

        // 已停用（status=1）；每种情况换一个 appKey，避免命中上一步缓存的结果
        registerOpenApp("off-app", "off-secret", 14L, 7L, 1, 50, null, null, "POST " + PATH);
        signedPost("off-app", "off-secret", PATH, "{}").exchange().expectStatus().isUnauthorized();

        // 已过期（过期时间为昨天）
        registerOpenApp("old-app", "old-secret", 15L, 7L, 0, 50, null, LocalDateTime.now().minusDays(1), "POST " + PATH);
        signedPost("old-app", "old-secret", PATH, "{}").exchange().expectStatus().isUnauthorized();

        assertThat(PAY.requests).isEmpty();

        // 过期时间在将来则正常放行
        registerOpenApp("new-app", "new-secret", 16L, 7L, 0, 50, null, LocalDateTime.now().plusDays(1), "POST " + PATH);
        signedPost("new-app", "new-secret", PATH, "{}").exchange().expectStatus().isOk();
    }

    @Test
    void 缺少签名头应返回401() {
        registerDemoOpenApp();
        webClient.post().uri(PATH).contentType(MediaType.APPLICATION_JSON).bodyValue(BODY)
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void 非open_api路径不走签名校验() {
        webClient.post().uri("/admin-api/system/auth/login").exchange().expectStatus().isOk();
        assertThat(SYSTEM.requests).hasSize(1);
    }

    // ========== 接口授权 ==========

    @Test
    void 未授权的路径或方法返回403_路径须精确匹配() {
        registerDemoOpenApp();
        signedPost("/open-api/pay/order/refund", "{}").exchange().expectStatus().isForbidden();
        signedGet(PATH).exchange().expectStatus().isForbidden();                       // 方法不符
        signedPost(PATH + "/", "{}").exchange().expectStatus().isForbidden();          // 多一个斜杠
        signedPost("/open-api/pay/order/Create", "{}").exchange().expectStatus().isForbidden();   // 大小写不同
        assertThat(PAY.requests).isEmpty();
    }

    @Test
    void 未授权返回403的CommonResult且下游不被调用() {
        registerDemoOpenApp();
        signedPost("/open-api/pay/order/refund", "{}").exchange().expectStatus().isForbidden()
                .expectBody().jsonPath("$.code").isEqualTo(403);
        assertThat(PAY.requests).isEmpty();
    }

    @Test
    void 授权检查在nonce之后_未授权的请求也消耗nonce() {
        registerDemoOpenApp();
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();
        String path = "/open-api/pay/order/refund";
        String sign = openApiSign(OPEN_APP_SECRET, "POST", path, null, timestamp, nonce,
                "{}".getBytes(StandardCharsets.UTF_8));

        post(path, "{}", timestamp, nonce, sign, OPEN_APP_ID).exchange().expectStatus().isForbidden();
        // 同一请求重放：nonce 已被占用，在授权检查之前就被拒绝
        post(path, "{}", timestamp, nonce, sign, OPEN_APP_ID).exchange().expectStatus().isUnauthorized();
    }

    // ========== IP 白名单 ==========

    @Test
    void IP不在白名单返回403_在白名单放行() {
        registerOpenApp("ip-app", "ip-secret", 12L, 7L, 0, 50, "10.0.0.0/8", null, "POST " + PATH);
        signedPost("ip-app", "ip-secret", PATH, "{}").exchange().expectStatus().isForbidden()
                .expectBody().jsonPath("$.code").isEqualTo(403);
        assertThat(PAY.requests).isEmpty();

        registerOpenApp("ip-app2", "ip-secret", 13L, 7L, 0, 50, "127.0.0.1, ::1", null, "POST " + PATH);
        signedPost("ip-app2", "ip-secret", PATH, "{}").exchange().expectStatus().isOk();
    }

    @Test
    void IP取自连接对端_伪造X_Forwarded_For不能绕过白名单() {
        registerOpenApp("xff-app", "xff-secret", 17L, 7L, 0, 50, "10.0.0.0/8", null, "POST " + PATH);
        // 请求实际来自 127.0.0.1，自称来自白名单内的 10.1.1.1
        signedPost("xff-app", "xff-secret", PATH, "{}")
                .header("X-Forwarded-For", "10.1.1.1")
                .exchange().expectStatus().isForbidden();

        // 反过来：白名单只有本机，自称来自别处也不影响
        registerOpenApp("xff-app2", "xff-secret", 18L, 7L, 0, 50, "127.0.0.1, ::1", null, "POST " + PATH);
        signedPost("xff-app2", "xff-secret", PATH, "{}")
                .header("X-Forwarded-For", "8.8.8.8")
                .exchange().expectStatus().isOk();
        assertThat(PAY.requests).hasSize(1);
    }

    @Test
    void IP白名单检查在验签之前_非白名单地址得到403而不是签名错误的401() {
        registerOpenApp("ip-app3", "ip-secret", 19L, 7L, 0, 50, "10.0.0.0/8", null, "POST " + PATH);
        post(PATH, BODY, System.currentTimeMillis(), UUID.randomUUID().toString(), "wrong-sign", "ip-app3")
                .exchange().expectStatus().isForbidden();
    }

    // ========== 应用缓存 ==========

    @Test
    void 应用快照被缓存_失效消息清除缓存() {
        registerDemoOpenApp();
        signedPost(PATH, "{}").exchange().expectStatus().isOk();
        signedPost(PATH, "{}").exchange().expectStatus().isOk();
        assertThat(OPEN.lookupCount(OPEN_APP_ID)).isEqualTo(1);

        // 应用被停用，但网关仍用缓存
        registerOpenApp(OPEN_APP_ID, OPEN_APP_SECRET, 11L, OPEN_APP_TENANT_ID, 1, 50, null, null, "POST " + PATH);
        signedPost(PATH, "{}").exchange().expectStatus().isOk();

        // 按 appKey 失效
        stringRedisTemplate.convertAndSend("ccloud:open:app:invalidate", OPEN_APP_ID).block();
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                signedPost(PATH, "{}").exchange().expectStatus().isUnauthorized());

        // 空串：全部失效
        registerDemoOpenApp();
        stringRedisTemplate.convertAndSend("ccloud:open:app:invalidate", "").block();
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                signedPost(PATH, "{}").exchange().expectStatus().isOk());
    }

    @Test
    void 失效某个appKey不影响其他应用的缓存() {
        registerDemoOpenApp();
        registerOpenApp("other-app", "other-secret", 20L, 7L, 0, 50, null, null, "POST " + PATH);
        signedPost(PATH, "{}").exchange().expectStatus().isOk();
        signedPost("other-app", "other-secret", PATH, "{}").exchange().expectStatus().isOk();

        stringRedisTemplate.convertAndSend("ccloud:open:app:invalidate", "other-app").block();
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            signedPost("other-app", "other-secret", PATH, "{}").exchange().expectStatus().isOk();
            assertThat(OPEN.lookupCount("other-app")).isEqualTo(2);
        });
        assertThat(OPEN.lookupCount(OPEN_APP_ID)).isEqualTo(1);
    }

    @Test
    void open不可用且未缓存返回503() {
        OPEN.openApps.put("down-app", new StubServer.TokenReply(500, "oops"));
        signedPost("down-app", "x", PATH, "{}").exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        // 失败不缓存：open 恢复后立即可用
        registerOpenApp("down-app", "x", 21L, 7L, 0, 50, null, null, "POST " + PATH);
        signedPost("down-app", "x", PATH, "{}").exchange().expectStatus().isOk();
    }

    // ========== 内部身份头 ==========

    @Test
    void 外部伪造的内部身份头被剥离() throws Exception {
        registerDemoOpenApp();
        String forged = Base64.getEncoder().encodeToString(
                "{\"id\":1,\"userType\":2,\"tenantId\":1}".getBytes(StandardCharsets.UTF_8));
        signedPost(PATH, "{}")
                .header("X-Login-User", forged)
                .header("X-Login-User-Sign", "forged-sign")
                .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                .header("X-Tenant-Id", "1")
                .exchange().expectStatus().isOk();

        Headers headers = PAY.requests.get(0).headers();
        assertThat(headers.get("X-Login-User")).hasSize(1);
        assertThat(headers.getFirst("X-Login-User")).isNotEqualTo(forged);
        assertThat(headers.getFirst("X-Login-User-Sign")).isNotEqualTo("forged-sign");
        assertThat(headers.get("X-Tenant-Id")).containsExactly(String.valueOf(OPEN_APP_TENANT_ID));
        LoginUser user = decodeLoginUser(headers);
        assertThat(user.getId()).isEqualTo(11L);
        assertThat(user.getUserType()).isEqualTo(UserTypeEnum.OPEN_APP.getValue());
        assertThat(user.getTenantId()).isEqualTo(OPEN_APP_TENANT_ID);
    }

    /**
     * 解码 X-Login-User，并按协议独立校验签名：Base64(HmacSHA256(secret, X-Login-User + "." + X-Timestamp))
     */
    private static LoginUser decodeLoginUser(Headers headers) throws Exception {
        String payload = headers.getFirst("X-Login-User");
        String timestamp = headers.getFirst("X-Timestamp");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = Base64.getEncoder().encodeToString(
                mac.doFinal((payload + "." + timestamp).getBytes(StandardCharsets.UTF_8)));
        assertThat(headers.getFirst("X-Login-User-Sign")).isEqualTo(expected);
        return JsonUtils.getObjectMapper().readValue(Base64.getDecoder().decode(payload), LoginUser.class);
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
