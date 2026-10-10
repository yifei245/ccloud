package com.ccloud.module.open.support;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 测试用回调接收方：记录收到的请求，按 status 应答；redirectTo 不为空时返回 302
 */
public final class NotifyReceiver implements AutoCloseable {

    public record Received(Headers headers, String body) {
    }

    public final List<Received> requests = new CopyOnWriteArrayList<>();
    public volatile int status = 200;
    public volatile String responseBody = "success";
    public volatile String redirectTo;
    public volatile long delayMillis;

    private final HttpServer server;

    public NotifyReceiver() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            Headers headers = new Headers();
            headers.putAll(exchange.getRequestHeaders());
            requests.add(new Received(headers, new String(exchange.getRequestBody().readAllBytes(), UTF_8)));
            if (delayMillis > 0) {
                try { Thread.sleep(delayMillis); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
            if (redirectTo != null) {
                exchange.getResponseHeaders().add("Location", redirectTo);
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }
            byte[] bytes = responseBody.getBytes(UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public String url(String path) {
        return "http://127.0.0.1:" + port() + path;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
