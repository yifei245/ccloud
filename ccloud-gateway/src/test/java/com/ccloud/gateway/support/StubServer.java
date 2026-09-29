package com.ccloud.gateway.support;

import com.ccloud.framework.common.util.json.JsonUtils;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 下游服务桩：记录收到的业务请求（路径、请求头、请求体），返回 {"server": 名称}。
 * system 桩另外应答令牌校验 RPC，结果按令牌从 {@link #tokens} 取，未登记的令牌返回 401
 */
public final class StubServer {

    public static final String TOKEN_CHECK_PATH = "/rpc-api/system/oauth2/token/check";

    public record Received(String method, String pathAndQuery, Headers headers, String body) {
    }

    /**
     * 令牌校验的应答：HTTP 状态码与响应体
     */
    public record TokenReply(int status, String body) {
    }

    public final List<Received> requests = new CopyOnWriteArrayList<>();
    public final Map<String, TokenReply> tokens = new ConcurrentHashMap<>();
    public final Map<String, AtomicInteger> tokenChecks = new ConcurrentHashMap<>();

    private final HttpServer server;

    public StubServer(String name) {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", exchange -> {
            if (TOKEN_CHECK_PATH.equals(exchange.getRequestURI().getPath())) {
                replyTokenCheck(exchange);
                return;
            }
            String query = exchange.getRequestURI().getRawQuery();
            Headers headers = new Headers();
            headers.putAll(exchange.getRequestHeaders());
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(new Received(exchange.getRequestMethod(),
                    exchange.getRequestURI().getRawPath() + (query != null ? "?" + query : ""), headers, body));
            write(exchange, 200, "{\"server\":\"" + name + "\"}");
        });
        server.start();
    }

    private void replyTokenCheck(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        String token = query == null ? "" : URLDecoder.decode(query.replaceFirst("^accessToken=", ""), StandardCharsets.UTF_8);
        tokenChecks.computeIfAbsent(token, k -> new AtomicInteger()).incrementAndGet();
        TokenReply reply = tokens.getOrDefault(token,
                new TokenReply(200, JsonUtils.toJsonString(Map.of("code", 401, "msg", "访问令牌不存在"))));
        write(exchange, reply.status(), reply.body());
    }

    private static void write(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    public int checkCount(String token) {
        AtomicInteger count = tokenChecks.get(token);
        return count == null ? 0 : count.get();
    }

    public void reset() {
        requests.clear();
        tokens.clear();
        tokenChecks.clear();
    }

    public int port() {
        return server.getAddress().getPort();
    }
}
