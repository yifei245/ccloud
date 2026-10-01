package com.ccloud.module.infra.framework.file.core.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 测试用的进程内 S3 服务端：路径风格（/{bucket}/{key}），对象存在内存里，只实现 PUT、GET、DELETE，并记录收到的每个请求。
 * 不校验签名：签名由 SDK 生成，测试只断言请求里带了签名。
 */
class FakeS3Server {

    private static final String NO_SUCH_KEY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Error><Code>NoSuchKey</Code><Message>The specified key does not exist.</Message></Error>";

    /**
     * @param headers 请求头，名称按不区分大小写比较
     */
    record RecordedRequest(String method, String path, Map<String, String> headers, byte[] body) {

        String header(String name) {
            return headers.get(name);
        }

        List<String> headerNames() {
            return new ArrayList<>(headers.keySet());
        }
    }

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();
    private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;

    void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", this::handle);
        server.start();
    }

    void stop() {
        server.stop(0);
    }

    String endpoint() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    List<RecordedRequest> requests() {
        return Collections.unmodifiableList(requests);
    }

    void clear() {
        requests.clear();
        objects.clear();
    }

    private static boolean isAwsChunked(Map<String, String> headers) {
        String sha256 = headers.get("x-amz-content-sha256");
        return sha256 != null && sha256.startsWith("STREAMING-");
    }

    /**
     * aws-chunked 编码：每块为 "{十六进制长度};chunk-signature={签名}\r\n{数据}\r\n"，以长度 0 的块结束
     */
    private static byte[] decodeAwsChunked(byte[] raw) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int pos = 0;
        while (pos < raw.length) {
            int lineEnd = indexOfCrlf(raw, pos);
            String header = new String(raw, pos, lineEnd - pos, StandardCharsets.US_ASCII);
            int size = Integer.parseInt(header.split(";")[0].trim(), 16);
            if (size == 0) {
                break;
            }
            out.write(raw, lineEnd + 2, size);
            pos = lineEnd + 2 + size + 2;
        }
        return out.toByteArray();
    }

    private static int indexOfCrlf(byte[] raw, int from) {
        for (int i = from; i < raw.length - 1; i++) {
            if (raw[i] == '\r' && raw[i + 1] == '\n') {
                return i;
            }
        }
        throw new IllegalArgumentException("aws-chunked 内容格式不正确");
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            byte[] raw = exchange.getRequestBody().readAllBytes();
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getRawPath();
            Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            exchange.getRequestHeaders().forEach((name, values) -> headers.put(name, values.get(0)));
            // 走 http 时 SDK 用 aws-chunked 分块签名上传，真实 S3 会还原出原始内容
            byte[] body = isAwsChunked(headers) ? decodeAwsChunked(raw) : raw;
            requests.add(new RecordedRequest(method, path, headers, body));

            switch (method) {
                case "PUT" -> {
                    objects.put(path, body);
                    exchange.getResponseHeaders().add("ETag", "\"fake-etag\"");
                    exchange.sendResponseHeaders(200, -1);
                }
                case "GET" -> {
                    byte[] content = objects.get(path);
                    if (content == null) {
                        byte[] error = NO_SUCH_KEY.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "application/xml");
                        exchange.sendResponseHeaders(404, error.length);
                        exchange.getResponseBody().write(error);
                    } else {
                        exchange.sendResponseHeaders(200, content.length);
                        exchange.getResponseBody().write(content);
                    }
                }
                case "DELETE" -> {
                    objects.remove(path);
                    exchange.sendResponseHeaders(204, -1);
                }
                default -> exchange.sendResponseHeaders(400, -1);
            }
        }
    }
}
