package com.ccloud.gateway.filter.security;

import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.gateway.support.GatewayIntegrationTestSupport;
import com.ccloud.gateway.support.StubServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.Headers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TokenAuthenticationFilterTest extends GatewayIntegrationTestSupport {

    // ========== 白名单 ==========

    @Test
    void 白名单路径无token也放行且不写内部头() {
        webClient.post().uri("/admin-api/system/auth/login").exchange()
                .expectStatus().isOk();

        Headers headers = lastRequest(SYSTEM).headers();
        assertThat(headers.getFirst("X-Login-User")).isNull();
        assertThat(headers.getFirst("X-Login-User-Sign")).isNull();
    }

    /**
     * 白名单只表示"可以匿名"，不表示"忽略身份"：发送修改密码验证码、SSO 授权检查等接口
     * 匿名可调，登录后调用时下游需要知道是谁
     */
    @Test
    void 白名单路径携带有效token时仍应写入内部头() {
        String token = registerToken(5L, 1, TENANT_ID, LocalDateTime.now().plusHours(1));

        webClient.post().uri("/app-api/member/auth/send-sms-code")
                .headers(h -> h.setBearerAuth(token))
                .exchange().expectStatus().isOk();

        assertThat(lastRequest(MEMBER).headers().getFirst("X-Login-User")).isNotNull();
    }

    @Test
    void 白名单路径携带无效token时按匿名放行_避免过期令牌挡住登录() {
        webClient.post().uri("/admin-api/system/auth/login")
                .headers(h -> h.setBearerAuth("expired-or-unknown"))
                .exchange().expectStatus().isOk();

        assertThat(lastRequest(SYSTEM).headers().getFirst("X-Login-User")).isNull();
    }

    @Test
    void 白名单按HTTP方法区分_SSO授权确认的POST仍需登录() {
        webClient.get().uri("/admin-api/system/sso/authorize?clientId=x").exchange().expectStatus().isOk();
        webClient.post().uri("/admin-api/system/sso/authorize").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void 白名单只覆盖登录前接口_member修改密码需要登录() {
        webClient.post().uri("/app-api/member/auth/update-password").exchange()
                .expectStatus().isUnauthorized();
        assertThat(MEMBER.requests).isEmpty();
    }

    // ========== 令牌校验 ==========

    @Test
    void 有效token应写入四个内部头且签名可被下游验证通过() throws Exception {
        String token = registerToken(100L, 2, TENANT_ID, LocalDateTime.now().plusHours(1));

        webClient.get().uri("/admin-api/system/user/page")
                .headers(h -> h.setBearerAuth(token))
                .exchange().expectStatus().isOk();

        Headers headers = lastRequest(SYSTEM).headers();
        String payload = headers.getFirst("X-Login-User");
        String timestamp = headers.getFirst("X-Timestamp");
        String sign = headers.getFirst("X-Login-User-Sign");
        assertThat(headers.getFirst("X-Tenant-Id")).isEqualTo(String.valueOf(TENANT_ID));
        assertThat(Long.parseLong(timestamp)).isCloseTo(System.currentTimeMillis(),
                org.assertj.core.data.Offset.offset(10_000L));
        // 按协议独立计算：Base64(HmacSHA256(secret, X-Login-User + "." + X-Timestamp))
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = Base64.getEncoder().encodeToString(
                mac.doFinal((payload + "." + timestamp).getBytes(StandardCharsets.UTF_8)));
        assertThat(sign).isEqualTo(expected);

        JsonNode loginUser = JsonUtils.getObjectMapper().readTree(Base64.getDecoder().decode(payload));
        assertThat(loginUser.get("id").asLong()).isEqualTo(100L);
        assertThat(loginUser.get("userType").asInt()).isEqualTo(2);
        assertThat(loginUser.get("tenantId").asLong()).isEqualTo(TENANT_ID);
        assertThat(loginUser.get("clientId").asText()).isEqualTo("ccloud-web");
        assertThat(loginUser.get("scopes").get(0).asText()).isEqualTo("user.read");
        assertThat(loginUser.get("info").get("nickname").asText()).isEqualTo("芋艿");
        // 下游 LoginUser 按同一格式反序列化并二次校验过期时间
        assertThat(loginUser.get("expiresTime").asText()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        // 原样转发 Authorization：登出接口需要原始令牌
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + token);
    }

    @Test
    void 无效token应返回401的CommonResult() {
        webClient.get().uri("/admin-api/system/user/page")
                .headers(h -> h.setBearerAuth("not-exists"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.code").isEqualTo(401);

        assertThat(SYSTEM.requests).isEmpty();
    }

    @Test
    void 缺少Authorization头且非白名单应返回401() {
        webClient.get().uri("/admin-api/system/user/page").exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.code").isEqualTo(401);

        assertThat(SYSTEM.requests).isEmpty();
    }

    @Test
    void 非Bearer格式的Authorization按未携带令牌处理() {
        webClient.get().uri("/admin-api/system/user/page")
                .header(HttpHeaders.AUTHORIZATION, "Basic YWRtaW46YWRtaW4=")
                .exchange().expectStatus().isUnauthorized();

        assertThat(SYSTEM.tokenChecks).isEmpty();
    }

    // ========== 缓存 ==========

    @Test
    void 秒内重复请求同一token只调用一次check_token() {
        String token = adminToken();

        for (int i = 0; i < 3; i++) {
            webClient.get().uri("/admin-api/system/user/page")
                    .headers(h -> h.setBearerAuth(token))
                    .exchange().expectStatus().isOk();
        }

        assertThat(SYSTEM.checkCount(token)).isEqualTo(1);
    }

    @Test
    void token过期后缓存失效应重新校验() throws InterruptedException {
        String token = registerToken(1L, 2, TENANT_ID, LocalDateTime.now().plusSeconds(2));
        webClient.get().uri("/admin-api/system/user/page").headers(h -> h.setBearerAuth(token))
                .exchange().expectStatus().isOk();
        // 令牌过期后 system 不再认可它
        Thread.sleep(2_500);
        SYSTEM.tokens.remove(token);

        webClient.get().uri("/admin-api/system/user/page").headers(h -> h.setBearerAuth(token))
                .exchange().expectStatus().isUnauthorized();

        assertThat(SYSTEM.checkCount(token)).isEqualTo(2);
    }

    @Test
    void 登出后应清除该令牌的缓存_不再放行() {
        String token = adminToken();
        webClient.get().uri("/admin-api/system/user/page").headers(h -> h.setBearerAuth(token))
                .exchange().expectStatus().isOk();

        webClient.post().uri("/admin-api/system/auth/logout").headers(h -> h.setBearerAuth(token))
                .exchange().expectStatus().isOk();
        SYSTEM.tokens.remove(token);

        webClient.get().uri("/admin-api/system/user/page").headers(h -> h.setBearerAuth(token))
                .exchange().expectStatus().isUnauthorized();
    }

    // ========== system 不可用 ==========

    @Test
    void system服务不可用时应返回503而非500() {
        SYSTEM.tokens.put("boom", new StubServer.TokenReply(500, "{\"error\":\"down\"}"));

        webClient.get().uri("/admin-api/system/user/page").headers(h -> h.setBearerAuth("boom"))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.code").isEqualTo(503);
    }

    @Test
    void 校验返回非401的业务错误码也视为不可用_不能当作令牌无效() {
        SYSTEM.tokens.put("busy", new StubServer.TokenReply(200,
                JsonUtils.toJsonString(Map.of("code", 500, "msg", "系统异常"))));

        webClient.get().uri("/admin-api/system/user/page").headers(h -> h.setBearerAuth("busy"))
                .exchange().expectStatus().isEqualTo(503);
    }

    @Test
    void system不可用的结果不应被缓存_恢复后立即可用() {
        String token = adminToken();
        StubServer.TokenReply valid = SYSTEM.tokens.get(token);
        SYSTEM.tokens.put(token, new StubServer.TokenReply(500, "{}"));
        webClient.get().uri("/admin-api/system/user/page").headers(h -> h.setBearerAuth(token))
                .exchange().expectStatus().isEqualTo(503);

        SYSTEM.tokens.put(token, valid);
        webClient.get().uri("/admin-api/system/user/page").headers(h -> h.setBearerAuth(token))
                .exchange().expectStatus().isOk();
    }

    private static StubServer.Received lastRequest(StubServer server) {
        assertThat(server.requests).isNotEmpty();
        return server.requests.get(server.requests.size() - 1);
    }
}
