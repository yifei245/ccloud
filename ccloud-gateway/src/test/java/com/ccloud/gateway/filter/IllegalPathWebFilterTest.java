package com.ccloud.gateway.filter;

import com.ccloud.gateway.support.GatewayIntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 对 unreserved 字符的百分号编码一律拒绝。路由按解码后的路径段匹配，而网关的前缀判断与下游的 URL 前缀校验
 * 按原样字符串进行：{@code /%6Fpen-api/...} 若放行，会被路由到开放接口却不经签名、白名单与授权校验
 */
class IllegalPathWebFilterTest extends GatewayIntegrationTestSupport {

    @Test
    void 编码的open_api前缀被拒绝_带令牌与不带令牌都不会到达下游() {
        registerDemoOpenApp();
        String token = registerToken(100L, 1, TENANT_ID, LocalDateTime.now().plusHours(1));
        for (String path : List.of("/%6Fpen-api/pay/order/create", "/%6fpen-api/pay/order/create",
                "/open%2Dapi/pay/order/create", "/open-api/%70ay/order/create", "/open-api/pay/order/%63reate")) {
            webClient.post().uri(raw(path)).exchange()
                    .expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.code").isEqualTo(400);
            webClient.post().uri(raw(path)).headers(h -> h.setBearerAuth(token)).exchange()
                    .expectStatus().isBadRequest();
        }

        assertThat(PAY.requests).isEmpty();
    }

    @Test
    void 编码的admin_api前缀配会员令牌不会到达下游() {
        String memberToken = registerToken(100L, 1, TENANT_ID, LocalDateTime.now().plusHours(1));

        webClient.get().uri(raw("/%61dmin-api/system/user/page")).headers(h -> h.setBearerAuth(memberToken))
                .exchange().expectStatus().isBadRequest();
        webClient.get().uri(raw("/admin-api/%73ystem/user/page")).headers(h -> h.setBearerAuth(memberToken))
                .exchange().expectStatus().isBadRequest();

        assertThat(SYSTEM.requests).isEmpty();
    }

    @Test
    void 合法编码不受影响_查询串中的中文与路径中的空格() {
        String token = adminToken();

        webClient.get().uri(raw("/admin-api/system/user/page?name=%E4%B8%AD%61")).headers(h -> h.setBearerAuth(token))
                .exchange().expectStatus().isOk();
        webClient.get().uri(raw("/admin-api/system/user/a%20b")).headers(h -> h.setBearerAuth(token))
                .exchange().expectStatus().isOk();
        webClient.get().uri(raw("/admin-api/system/user/%E4%B8%AD")).headers(h -> h.setBearerAuth(token))
                .exchange().expectStatus().isOk();

        assertThat(SYSTEM.requests).hasSize(3);
    }

    @Test
    void unreserved字符的编码十六进制大小写都识别() {
        assertThat(IllegalPathWebFilter.isIllegal("/%6Fpen")).isTrue();
        assertThat(IllegalPathWebFilter.isIllegal("/%6fpen")).isTrue();
        assertThat(IllegalPathWebFilter.isIllegal("/a%2Db")).isTrue();
        assertThat(IllegalPathWebFilter.isIllegal("/a%2db")).isTrue();
        assertThat(IllegalPathWebFilter.isIllegal("/a%7E")).isTrue();
        assertThat(IllegalPathWebFilter.isIllegal("/a%5F")).isTrue();
        assertThat(IllegalPathWebFilter.isIllegal("/a%30")).isTrue();
        assertThat(IllegalPathWebFilter.isIllegal("/a%20b")).isFalse();
        assertThat(IllegalPathWebFilter.isIllegal("/a%E4%B8%AD")).isFalse();
        assertThat(IllegalPathWebFilter.isIllegal("/a%")).isFalse();
        assertThat(IllegalPathWebFilter.isIllegal("/a%6")).isFalse();
    }
}
