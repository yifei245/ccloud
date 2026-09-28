package com.ccloud.gateway.swagger;

import com.ccloud.gateway.support.GatewayIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "ccloud.swagger.enable=false")
class SwaggerDisabledTest extends GatewayIntegrationTestSupport {

    @Test
    void 关闭开关后doc_html应返回404() {
        webClient.get().uri("/doc.html").exchange().expectStatus().isNotFound();
        webClient.get().uri("/webjars/js/app.c31badf5.js").exchange().expectStatus().isNotFound();
        webClient.get().uri("/v3/api-docs/swagger-config").exchange().expectStatus().isNotFound();
        webClient.get().uri("/system/v3/api-docs/admin").exchange().expectStatus().isNotFound();

        assertThat(SYSTEM.requests).isEmpty();
    }
}
