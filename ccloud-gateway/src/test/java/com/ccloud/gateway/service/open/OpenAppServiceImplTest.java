package com.ccloud.gateway.service.open;

import com.ccloud.gateway.filter.openapi.OpenApiProperties;
import com.ccloud.gateway.support.GatewayIntegrationTestSupport;
import com.ccloud.gateway.support.StubServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * 应用快照读取与缓存：存在的应用缓存较久，不存在的短暂缓存，失败不缓存
 */
class OpenAppServiceImplTest extends GatewayIntegrationTestSupport {

    private static final String API = "POST /open-api/pay/order/create";

    @Autowired
    private OpenAppService openAppService;

    @Test
    void 解析open返回的快照_Long为字符串_时间为固定格式() {
        LocalDateTime expireTime = LocalDateTime.now().plusDays(1).withNano(0);
        registerOpenApp("snap-app", "snap-secret", 123456789012345678L, 7L, 0, 30, "10.0.0.0/8", expireTime,
                API, "GET /open-api/member/user/get-by-mobile");

        OpenApp app = openAppService.getApp("snap-app").block();

        assertThat(app).isNotNull();
        assertThat(app.getId()).isEqualTo(123456789012345678L);
        assertThat(app.getAppKey()).isEqualTo("snap-app");
        assertThat(app.getAppSecret()).isEqualTo("snap-secret");
        assertThat(app.getTenantId()).isEqualTo(7L);
        assertThat(app.getStatus()).isZero();
        assertThat(app.getRateLimit()).isEqualTo(30);
        assertThat(app.getIpWhitelist()).isEqualTo("10.0.0.0/8");
        assertThat(app.getExpireTime()).isEqualTo(expireTime);
        assertThat(app.getApis()).extracting(OpenApp.Api::getMethod, OpenApp.Api::getPath).containsExactly(
                org.assertj.core.groups.Tuple.tuple("POST", "/open-api/pay/order/create"),
                org.assertj.core.groups.Tuple.tuple("GET", "/open-api/member/user/get-by-mobile"));
        assertThat(app.isEnabled()).isTrue();
        assertThat(app.isExpired()).isFalse();
    }

    @Test
    void 应用不存在为空_且结果被缓存() {
        assertThat(openAppService.getApp("ghost").blockOptional()).isEmpty();
        assertThat(openAppService.getApp("ghost").blockOptional()).isEmpty();

        assertThat(OPEN.lookupCount("ghost")).isEqualTo(1);
    }

    @Test
    void 不存在的应用只短暂缓存_存在的应用缓存更久() {
        OpenApiProperties properties = new OpenApiProperties();
        properties.setAppCacheTtl(Duration.ofSeconds(60));
        properties.setAppCacheNotFoundTtl(Duration.ofMillis(300));
        OpenAppService service = new OpenAppServiceImpl(redirectedToStub(), properties);
        registerOpenApp("exists", "s", 1L, 7L, 0, 50, null, null, API);

        assertThat(service.getApp("exists").block()).isNotNull();
        assertThat(service.getApp("later").blockOptional()).isEmpty();
        assertThat(service.getApp("later").blockOptional()).isEmpty();
        assertThat(OPEN.lookupCount("later")).isEqualTo(1);

        // 不存在的应用过了短 TTL 重新查询，新建的应用得以尽快可用
        registerOpenApp("later", "s", 2L, 7L, 0, 50, null, null, API);
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(service.getApp("later").blockOptional()).isPresent());
        // 存在的应用仍在缓存中
        assertThat(service.getApp("exists").block()).isNotNull();
        assertThat(OPEN.lookupCount("exists")).isEqualTo(1);
    }

    @Test
    void open返回错误状态_错误码_无法解析的响应都视为不可用且不缓存() {
        OPEN.openApps.put("bad", new StubServer.TokenReply(500, "oops"));
        assertThatThrownBy(() -> openAppService.getApp("bad").block()).isInstanceOf(OpenAppUnavailableException.class);

        OPEN.openApps.put("bad", new StubServer.TokenReply(200, "{\"code\":500,\"msg\":\"内部错误\",\"data\":null}"));
        assertThatThrownBy(() -> openAppService.getApp("bad").block()).isInstanceOf(OpenAppUnavailableException.class);

        OPEN.openApps.put("bad", new StubServer.TokenReply(200, "not json"));
        assertThatThrownBy(() -> openAppService.getApp("bad").block()).isInstanceOf(OpenAppUnavailableException.class);

        // 三次失败都没有被缓存，open 恢复后立即可用
        assertThat(OPEN.lookupCount("bad")).isEqualTo(3);
        registerOpenApp("bad", "s", 3L, 7L, 0, 50, null, null, API);
        assertThat(openAppService.getApp("bad").block()).isNotNull();
    }

    @Test
    void invalidate只清除指定应用_invalidateAll清除全部() {
        registerOpenApp("a", "s", 1L, 7L, 0, 50, null, null, API);
        registerOpenApp("b", "s", 2L, 7L, 0, 50, null, null, API);
        openAppService.getApp("a").block();
        openAppService.getApp("b").block();

        openAppService.invalidate("a");
        openAppService.getApp("a").block();
        openAppService.getApp("b").block();
        assertThat(OPEN.lookupCount("a")).isEqualTo(2);
        assertThat(OPEN.lookupCount("b")).isEqualTo(1);

        openAppService.invalidateAll();
        openAppService.getApp("a").block();
        openAppService.getApp("b").block();
        assertThat(OPEN.lookupCount("a")).isEqualTo(3);
        assertThat(OPEN.lookupCount("b")).isEqualTo(2);
    }

    @Test
    void 接口授权按方法加原始路径精确匹配() {
        OpenApp app = new OpenApp();
        OpenApp.Api api = new OpenApp.Api();
        api.setMethod("POST");
        api.setPath("/open-api/pay/order/create");
        app.setApis(java.util.List.of(api));

        assertThat(app.allows("POST", "/open-api/pay/order/create")).isTrue();
        assertThat(app.allows("post", "/open-api/pay/order/create")).isTrue();
        assertThat(app.allows("GET", "/open-api/pay/order/create")).isFalse();
        assertThat(app.allows("POST", "/open-api/pay/order/create/")).isFalse();
        assertThat(app.allows("POST", "/open-api/pay/order/%63reate")).isFalse();
        assertThat(app.allows("POST", "/open-api/pay/order/create/../create")).isFalse();
    }

    @Test
    void 状态与过期判断() {
        OpenApp app = new OpenApp();
        app.setStatus(0);
        assertThat(app.isEnabled()).isTrue();
        assertThat(app.isExpired()).isFalse();           // 长期有效
        app.setExpireTime(LocalDateTime.now().plusMinutes(1));
        assertThat(app.isExpired()).isFalse();
        app.setExpireTime(LocalDateTime.now().minusSeconds(1));
        assertThat(app.isExpired()).isTrue();
        app.setStatus(1);
        assertThat(app.isEnabled()).isFalse();
    }

    @Test
    void toString不含密钥() {
        OpenApp app = new OpenApp();
        app.setAppKey("k");
        app.setAppSecret("super-secret-value");

        assertThat(app.toString()).contains("k").doesNotContain("super-secret-value");
    }

    /**
     * 把对 ccloud-open-server 的请求改写到桩，用于不经过负载均衡单独构造服务实例
     */
    private static WebClient.Builder redirectedToStub() {
        return WebClient.builder().filter((request, next) -> next.exchange(ClientRequest.from(request)
                .url(URI.create("http://127.0.0.1:" + OPEN.port() + request.url().getRawPath() + "?"
                        + request.url().getRawQuery()))
                .build()));
    }
}
