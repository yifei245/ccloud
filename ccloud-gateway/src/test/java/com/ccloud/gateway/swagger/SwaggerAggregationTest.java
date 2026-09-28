package com.ccloud.gateway.swagger;

import com.ccloud.gateway.support.GatewayIntegrationTestSupport;
import com.ccloud.gateway.support.StubServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerAggregationTest extends GatewayIntegrationTestSupport {

    @Test
    void doc_html应可访问() {
        webClient.get().uri("/doc.html").exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);
    }

    @Test
    void swagger_resources应列出已注册的服务分组() {
        webClient.get().uri("/v3/api-docs/swagger-config").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.urls[?(@.url == '/system/v3/api-docs/admin')]").exists()
                .jsonPath("$.urls[?(@.url == '/system/v3/api-docs/app')]").exists()
                .jsonPath("$.urls[?(@.url == '/system/v3/api-docs/open')]").exists()
                .jsonPath("$.urls[?(@.url == '/member/v3/api-docs/app')]").exists()
                .jsonPath("$.urls[?(@.url == '/pay/v3/api-docs/admin')]").exists()
                // 内部接口分组不经网关展示；注册中心里的非业务服务（网关自身）不列出
                .jsonPath("$.urls[?(@.url =~ /.*rpc.*/)]").doesNotExist()
                .jsonPath("$.urls[?(@.url =~ /.*gateway.*/)]").doesNotExist()
                // 名称符合约定但网关没有它的路由，列出来也只会是打不开的分组
                .jsonPath("$.urls[?(@.url =~ /.*unknown.*/)]").doesNotExist();
    }

    @Test
    void 分组文档经网关转发到对应服务的v3_api_docs() {
        webClient.get().uri("/system/v3/api-docs/admin").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.server").isEqualTo("system");

        assertThat(SYSTEM.requests).extracting(StubServer.Received::pathAndQuery).containsExactly("/v3/api-docs/admin");
    }

    /**
     * 文档路由会改写路径，只能改写成 /v3/api-docs/{admin,app,open}，否则可借它访问下游任意路径
     */
    @Test
    void 文档路由不能被借来访问下游其他路径() {
        webClient.get().uri("/system/v3/api-docs/rpc").exchange().expectStatus().isNotFound();
        webClient.get().uri("/system/rpc-api/system/user/get").exchange().expectStatus().isNotFound();
        webClient.get().uri("/system/actuator/env").exchange().expectStatus().isNotFound();

        assertThat(SYSTEM.requests).isEmpty();
    }
}
