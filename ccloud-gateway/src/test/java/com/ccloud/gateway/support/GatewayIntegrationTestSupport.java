package com.ccloud.gateway.support;

import com.ccloud.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * 网关集成测试基类：真实 Redis，下游 system / member / pay 为 {@link StubServer}，经 simple discovery 注册。
 * 所有子类共享同一个 Spring 上下文
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("unit-test")
public abstract class GatewayIntegrationTestSupport {

    protected static final String SECRET = "unit-test-secret";
    protected static final long TENANT_ID = 1L;

    protected static final StubServer SYSTEM = new StubServer("system");
    protected static final StubServer MEMBER = new StubServer("member");
    protected static final StubServer PAY = new StubServer("pay");
    /**
     * promotion 路由在测试配置中限流阈值很低，专供限流测试使用
     */
    protected static final StubServer PROMOTION = new StubServer("promotion");

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS = startRedis();

    private static GenericContainer<?> startRedis() {
        GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
        redis.start();
        return redis;
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        Map.of("ccloud-system-server", SYSTEM, "ccloud-member-server", MEMBER, "ccloud-pay-server", PAY,
                        "ccloud-promotion-server", PROMOTION,
                        // 注册中心里的非业务服务、未登记路由的服务：文档聚合不应列出
                        "ccloud-gateway", SYSTEM, "ccloud-unknown-server", SYSTEM)
                .forEach((service, server) -> registry.add(
                        "spring.cloud.discovery.client.simple.instances." + service + "[0].uri",
                        () -> "http://127.0.0.1:" + server.port()));
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    protected WebTestClient webClient;
    @LocalServerPort
    protected int port;

    @BeforeEach
    void resetStubs() {
        SYSTEM.reset();
        MEMBER.reset();
        PAY.reset();
        PROMOTION.reset();
    }

    /**
     * 原样保留编码与 ..，不经 UriBuilder 规范化。WebTestClient 只对字符串路径补全主机，URI 对象须是绝对地址
     */
    protected URI raw(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    // ========== 开放接口签名 ==========

    protected static final String OPEN_APP_ID = "demo-app";
    protected static final String OPEN_APP_SECRET = "demo-secret";
    protected static final long OPEN_APP_TENANT_ID = 7L;

    /**
     * 按开放接口签名协议计算签名，独立于网关实现，用于交叉验证协议
     */
    protected static String openApiSign(String secret, String method, String rawPath, String rawQuery,
                                        long timestamp, String nonce, byte[] body) {
        try {
            String bodyHash = java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(body));
            String stringToSign = method + "\n" + rawPath + "\n" + (rawQuery == null ? "" : rawQuery) + "\n"
                    + timestamp + "\n" + nonce + "\n" + bodyHash;
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA256"));
            return java.util.Base64.getEncoder().encodeToString(
                    mac.doFinal(stringToSign.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 构造一个签名正确的 POST 开放接口请求
     */
    protected WebTestClient.RequestHeadersSpec<?> signedPost(String rawPath, String body) {
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return webClient.post().uri(rawPath)
                .header("X-Open-App-Id", OPEN_APP_ID)
                .header("X-Open-Timestamp", String.valueOf(timestamp))
                .header("X-Open-Nonce", nonce)
                .header("X-Open-Sign", openApiSign(OPEN_APP_SECRET, "POST", rawPath, null, timestamp, nonce, bytes))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(bytes);
    }

    /**
     * 登记一个有效的管理员令牌。每个测试用新令牌，避免网关的令牌缓存在用例间串扰
     */
    protected String adminToken() {
        return registerToken(1L, 2, TENANT_ID, LocalDateTime.now().plusHours(1));
    }

    protected String registerToken(long userId, int userType, long tenantId, LocalDateTime expiresTime) {
        String token = "token-" + UUID.randomUUID();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("userType", userType);
        data.put("userInfo", Map.of("nickname", "芋艿"));
        data.put("tenantId", tenantId);
        data.put("clientId", "ccloud-web");
        data.put("scopes", List.of("user.read"));
        data.put("expiresTime", expiresTime);
        // 与 system 一致：经 JsonUtils 序列化，Long 为字符串、时间为 yyyy-MM-dd HH:mm:ss
        SYSTEM.tokens.put(token, new StubServer.TokenReply(200,
                JsonUtils.toJsonString(Map.of("code", 0, "msg", "", "data", data))));
        return token;
    }
}
