package com.ccloud.monitor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("unit-test")
class MonitorServerApplicationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "unit-test-password";

    @Autowired
    private TestRestTemplate restTemplate;
    @LocalServerPort
    private int port;

    @Test
    void 未认证访问接口应返回401() {
        ResponseEntity<String> response = restTemplate.exchange("/applications", HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void 未认证访问面板页面应跳转登录页() throws Exception {
        // JDK HttpClient 默认不跟随跳转，TestRestTemplate 会跟随跳转后返回登录页的 200
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                java.net.http.HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/"))
                        .header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE).build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        assertThat(URI.create(response.headers().firstValue(HttpHeaders.LOCATION).orElseThrow()).getPath())
                .isEqualTo("/login");
    }

    @Test
    void 登录页可匿名访问() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.TEXT_HTML));
        assertThat(restTemplate.exchange("/login", HttpMethod.GET, new HttpEntity<>(headers), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void 密码错误应返回401() {
        ResponseEntity<String> response = restTemplate.withBasicAuth(USERNAME, "wrong")
                .exchange("/applications", HttpMethod.GET, new HttpEntity<>(jsonHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * 注册接口不能匿名调用：否则任何人都能往面板里塞一个指向恶意地址的实例，
     * 面板随后会以自身身份去请求那个地址
     */
    @Test
    void 匿名注册实例应被拒绝() {
        HttpHeaders headers = jsonHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange("/instances", HttpMethod.POST, new HttpEntity<>(
                "{\"name\":\"evil\",\"healthUrl\":\"http://169.254.169.254/latest/meta-data\"}", headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void 健康检查可匿名访问_供容器编排探活() {
        ResponseEntity<String> response = restTemplate.exchange("/actuator/health", HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()), String.class);

        // 断言响应体而非只看状态码：被拦截时会跳转登录页，跟随跳转后同样是 200
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void 认证后应列出从注册中心发现的ccloud服务_不含自身与非ccloud服务() {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<String> response = restTemplate.withBasicAuth(USERNAME, PASSWORD)
                    .exchange("/applications", HttpMethod.GET, new HttpEntity<>(jsonHeaders()), String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"ccloud-system-server\"");
        });

        String body = restTemplate.withBasicAuth(USERNAME, PASSWORD)
                .exchange("/applications", HttpMethod.GET, new HttpEntity<>(jsonHeaders()), String.class).getBody();
        assertThat(body).doesNotContainIgnoringCase("\"ccloud-monitor\"").doesNotContainIgnoringCase("nacos-console");
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
