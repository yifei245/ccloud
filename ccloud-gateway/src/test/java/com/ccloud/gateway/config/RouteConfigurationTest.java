package com.ccloud.gateway.config;

import com.ccloud.gateway.support.GatewayIntegrationTestSupport;
import com.ccloud.gateway.support.StubServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 路由规则。下游服务用 JDK HttpServer 桩代替，经 simple discovery 注册，
 * 断言请求到达了哪个服务、路径是否原样转发（不做 StripPrefix）
 */
class RouteConfigurationTest extends GatewayIntegrationTestSupport {

    private static List<String> paths(StubServer server) {
        return server.requests.stream().map(StubServer.Received::pathAndQuery).toList();
    }

    // ========== 业务路由 ==========

    @Test
    void admin_api_system路径应路由到system服务_路径与参数原样转发() {
        webClient.get().uri("/admin-api/system/user/page?pageNo=1").headers(h -> h.setBearerAuth(adminToken())).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.server").isEqualTo("system");

        assertThat(paths(SYSTEM)).containsExactly("/admin-api/system/user/page?pageNo=1");
    }

    @Test
    void app_api_member路径应路由到member服务() {
        webClient.post().uri("/app-api/member/auth/login").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.server").isEqualTo("member");

        assertThat(paths(MEMBER)).containsExactly("/app-api/member/auth/login");
        assertThat(paths(SYSTEM)).isEmpty();
    }

    @Test
    void admin_api_member路径也应路由到member服务() {
        webClient.get().uri("/admin-api/member/user/page").headers(h -> h.setBearerAuth(adminToken())).exchange()
                .expectBody().jsonPath("$.server").isEqualTo("member");
    }

    @Test
    void open_api_pay路径应路由到pay服务() {
        signedPost("/open-api/pay/order/create", "{}").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.server").isEqualTo("pay");

        assertThat(paths(PAY)).containsExactly("/open-api/pay/order/create");
    }

    @Test
    void open_api未知模块应返回404_不能借模块名路由到任意注册服务() {
        webClient.get().uri("/open-api/gateway/actuator/env").exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo(404);
    }

    @Test
    void sso与oauth2路径应路由到system服务() {
        String token = adminToken();
        webClient.get().uri("/sso/authorize").headers(h -> h.setBearerAuth(token)).exchange()
                .expectBody().jsonPath("$.server").isEqualTo("system");
        webClient.post().uri("/oauth2/token").headers(h -> h.setBearerAuth(token)).exchange()
                .expectBody().jsonPath("$.server").isEqualTo("system");
    }

    // ========== 内部接口阻断 ==========

    @Test
    void rpc_api路径应直接返回403() {
        webClient.get().uri("/rpc-api/system/user/get?id=1").exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.code").isEqualTo(403);

        assertThat(paths(SYSTEM)).isEmpty();
    }

    @Test
    void 编码后的rpc_api路径同样应被阻断() {
        webClient.get().uri(raw("/%72pc-api/system/user/get?id=1")).exchange()
                .expectStatus().value(status -> assertThat(status).isIn(400, 403));

        assertThat(paths(SYSTEM)).isEmpty();
    }

    @Test
    void 路径穿越应被拒绝_否则可经业务路由绕到下游的rpc_api() {
        for (String path : List.of("/admin-api/system/../../rpc-api/system/user/get",
                "/admin-api/system/..%2F..%2Frpc-api/system/user/get",
                "/admin-api/system/%2e%2e/%2e%2e/rpc-api/system/user/get",
                "/admin-api/system/..%5C..%5Crpc-api/system/user/get",
                "/admin-api/system/user;/../../../rpc-api/system/user/get",
                // Tomcat 剥离路径参数后 ..; 即 ..
                "/admin-api/system/..;/..;/rpc-api/system/user/get")) {
            webClient.get().uri(raw(path)).exchange()
                    .expectStatus().value(status -> assertThat(status).as(path).isIn(400, 403, 404));
        }

        assertThat(paths(SYSTEM)).isEmpty();
    }

    @Test
    void 网关actuator只开放健康检查_不暴露环境变量与路由管理() {
        webClient.get().uri("/actuator/health").exchange().expectStatus().isOk();
        webClient.get().uri("/actuator/env").exchange().expectStatus().isNotFound();
        webClient.get().uri("/actuator/gateway/routes").exchange().expectStatus().isNotFound();
    }

    // ========== 统一错误响应 ==========

    @Test
    void 未匹配任何路由应返回404的CommonResult而非HTML() {
        webClient.get().uri("/nope").exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.code").isEqualTo(404)
                .jsonPath("$.msg").isNotEmpty();
    }

    @Test
    void 下游服务无可用实例应返回503的CommonResult() {
        webClient.get().uri("/admin-api/bpm/task/page").headers(h -> h.setBearerAuth(adminToken())).exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.code").isEqualTo(503);
    }

    // ========== CORS ==========

    @Test
    void 预检请求应返回CORS头() {
        webClient.method(HttpMethod.OPTIONS).uri("/admin-api/system/user/page")
                .header(HttpHeaders.ORIGIN, "https://admin.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,X-Tenant-Id")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://admin.example.com")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);

        assertThat(paths(SYSTEM)).as("预检不应转发到下游").isEmpty();
    }

    @Test
    void 跨域响应应暴露trace_id头() {
        webClient.get().uri("/admin-api/system/user/page")
                .headers(h -> h.setBearerAuth(adminToken()))
                .header(HttpHeaders.ORIGIN, "https://admin.example.com")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://admin.example.com")
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        value -> assertThat(value).containsIgnoringCase("trace-id"));
    }
}
