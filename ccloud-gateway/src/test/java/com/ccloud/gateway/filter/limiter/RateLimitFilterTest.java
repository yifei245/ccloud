package com.ccloud.gateway.filter.limiter;

import com.ccloud.gateway.support.GatewayIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 限流：Redis 令牌桶，多个网关实例共享配额。测试配置中 promotion 路由每秒补充 1 个令牌、桶容量 2，
 * 开放应用 limited-app（在 open 桩中登记）每秒 1 次
 */
class RateLimitFilterTest extends GatewayIntegrationTestSupport {

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    /**
     * 开放应用的令牌桶跨用例共享，每个用例从满桶开始
     */
    @BeforeEach
    void resetOpenAppBucket() {
        redisTemplate.delete("ccloud:rate_limit:{open-app:limited-app}.tokens",
                "ccloud:rate_limit:{open-app:limited-app}.timestamp").block();
    }

    // ========== 按路由 ==========

    @Test
    void 未超阈值时放行() {
        String token = uniqueUserToken();

        webClient.get().uri("/admin-api/promotion/coupon/page").headers(h -> h.setBearerAuth(token))
                .exchange().expectStatus().isOk();

        assertThat(PROMOTION.requests).hasSize(1);
    }

    /**
     * 令牌桶脚本按 Redis TIME 的整秒补充令牌，连续请求可能跨过一次秒边界多补一个令牌，
     * 因此发 桶容量 + 2 次，断言其中出现 429，而不是断言第几次被拒
     */
    @Test
    void 超过阈值应返回429且body为CommonResult() {
        String token = uniqueUserToken();
        EntityExchangeResult<byte[]> rejected = null;
        for (int i = 0; i < 4 && rejected == null; i++) {
            EntityExchangeResult<byte[]> result = webClient.get().uri("/admin-api/promotion/coupon/page")
                    .headers(h -> h.setBearerAuth(token)).exchange().expectBody().returnResult();
            if (result.getStatus().value() == 429) {
                rejected = result;
            }
        }

        assertThat(rejected).as("桶容量 2，连发 4 次应被拒").isNotNull();
        assertThat(PROMOTION.requests).hasSizeBetween(2, 3);
        assertThat(rejected.getResponseHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(rejected.getResponseHeaders().getFirst("Retry-After")).isEqualTo("1");
        assertThat(new String(rejected.getResponseBody(), StandardCharsets.UTF_8)).contains("\"code\":429");
    }

    @Test
    void 不同用户的配额互相独立_一个用户刷接口不影响其他用户() {
        String greedy = uniqueUserToken();
        for (int i = 0; i < 5; i++) {
            webClient.get().uri("/admin-api/promotion/coupon/page").headers(h -> h.setBearerAuth(greedy)).exchange();
        }

        webClient.get().uri("/admin-api/promotion/coupon/page").headers(h -> h.setBearerAuth(uniqueUserToken()))
                .exchange().expectStatus().isOk();
    }

    @Test
    void 未配置阈值的路由使用默认阈值_正常访问不受影响() {
        String token = uniqueUserToken();
        for (int i = 0; i < 5; i++) {
            webClient.get().uri("/admin-api/system/user/page").headers(h -> h.setBearerAuth(token))
                    .exchange().expectStatus().isOk();
        }
    }

    // ========== 开放应用 ==========

    @Test
    void 开放应用超过自身阈值应返回429() {
        registerLimitedApp();
        assertThat(exhaustLimitedApp()).contains(429);
    }

    @Test
    void 不同appKey的配额互相独立() {
        registerLimitedApp();
        registerDemoOpenApp();
        assertThat(exhaustLimitedApp()).contains(429);

        signedAs(OPEN_APP_ID, OPEN_APP_SECRET).exchange().expectStatus().isOk();
    }

    /**
     * limited-app 每秒 1 次、桶容量 1，连发 3 次至多跨一次秒边界，必然出现 429
     */
    private List<Integer> exhaustLimitedApp() {
        List<Integer> statuses = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            statuses.add(signedAs("limited-app", "limited-secret").exchange().returnResult(String.class)
                    .getStatus().value());
        }
        return statuses;
    }

    /**
     * 限流在验签之后：否则任何人拿别人的 appId 发一串无效请求，就能耗尽对方的配额
     */
    @Test
    void 签名无效的请求不消耗应用配额() {
        registerLimitedApp();
        for (int i = 0; i < 5; i++) {
            webClient.post().uri("/open-api/pay/order/create")
                    .header("X-Open-App-Id", "limited-app")
                    .header("X-Open-Timestamp", String.valueOf(System.currentTimeMillis()))
                    .header("X-Open-Nonce", UUID.randomUUID().toString())
                    .header("X-Open-Sign", "forged")
                    .contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                    .exchange().expectStatus().isUnauthorized();
        }

        signedAs("limited-app", "limited-secret").exchange().expectStatus().isOk();
    }

    private void registerLimitedApp() {
        registerOpenApp("limited-app", "limited-secret", 21L, 7L, 0, 1, null, null, "POST /open-api/pay/order/create");
    }

    private String uniqueUserToken() {
        return registerToken(Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000L), 2, TENANT_ID,
                LocalDateTime.now().plusHours(1));
    }

    private org.springframework.test.web.reactive.server.WebTestClient.RequestHeadersSpec<?> signedAs(
            String appId, String secret) {
        String path = "/open-api/pay/order/create";
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        return webClient.post().uri(path)
                .header("X-Open-App-Id", appId)
                .header("X-Open-Timestamp", String.valueOf(timestamp))
                .header("X-Open-Nonce", nonce)
                .header("X-Open-Sign", openApiSign(secret, "POST", path, null, timestamp, nonce, body))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }
}
