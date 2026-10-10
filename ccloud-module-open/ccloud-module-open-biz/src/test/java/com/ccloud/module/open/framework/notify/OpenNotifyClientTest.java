package com.ccloud.module.open.framework.notify;

import com.ccloud.module.open.framework.config.OpenProperties;
import com.ccloud.module.open.support.NotifyReceiver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenNotifyClientTest {

    private NotifyReceiver receiver;

    @BeforeEach
    void start() throws IOException {
        receiver = new NotifyReceiver();
    }

    @AfterEach
    void stop() {
        receiver.close();
    }

    private static OpenNotifyClient client(boolean allowPrivate) {
        OpenProperties.Notify notify = new OpenProperties.Notify();
        notify.setAllowPrivateAddress(allowPrivate);
        notify.setReadTimeout(Duration.ofSeconds(1));
        return new OpenNotifyClient(notify);
    }

    private int port() {
        return receiver.port();
    }

    @Test
    void 状态2xx为成功_请求头与请求体原样发送() {
        NotifyResult result = client(true).post(receiver.url("/n"), Map.of("X-Open-Event-Id", "e1"), "{\"a\":1}");
        assertThat(result.success()).isTrue();
        assertThat(result.httpStatus()).isEqualTo(200);
        assertThat(result.responseBody()).isEqualTo("success");
        assertThat(receiver.requests.get(0).headers().getFirst("X-Open-Event-Id")).isEqualTo("e1");
        assertThat(receiver.requests.get(0).headers().getFirst("Content-Type")).startsWith("application/json");
        assertThat(receiver.requests.get(0).body()).isEqualTo("{\"a\":1}");
    }

    @Test
    void 非2xx_超时_连接失败均为失败() {
        receiver.status = 500;
        assertThat(client(true).post(receiver.url("/n"), Map.of(), "{}")).satisfies(r -> {
            assertThat(r.success()).isFalse();
            assertThat(r.httpStatus()).isEqualTo(500);
        });
        receiver.status = 200;
        receiver.delayMillis = 2000;
        assertThat(client(true).post(receiver.url("/n"), Map.of(), "{}").success()).isFalse();
        assertThat(client(true).post("http://127.0.0.1:1/n", Map.of(), "{}").success()).isFalse();
    }

    @Test
    void 不跟随重定向() {
        receiver.redirectTo = "http://169.254.169.254/latest/meta-data";
        NotifyResult result = client(true).post(receiver.url("/n"), Map.of(), "{}");
        assertThat(result.success()).isFalse();
        assertThat(result.httpStatus()).isEqualTo(302);
        assertThat(receiver.requests).hasSize(1);
    }

    @Test
    void 默认拒绝回环与内网地址_字面量与域名都拦截_不发出请求() {
        OpenNotifyClient client = client(false);
        for (String url : List.of(receiver.url("/n"), "http://localhost:" + port() + "/n", "http://10.0.0.1/n",
                "http://192.168.1.1/n", "http://[::1]:" + port() + "/n", "http://169.254.169.254/n", "http://100.64.0.1/n")) {
            NotifyResult result = client.post(url, Map.of(), "{}");
            assertThat(result.success()).as(url).isFalse();
            assertThat(result.httpStatus()).as(url).isZero();
            assertThat(result.responseBody()).as(url).contains("受限地址");
        }
        assertThat(receiver.requests).isEmpty();
    }

    @Test
    void 允许内网时仍拒绝云元数据地址() {
        for (String url : List.of("http://169.254.169.254/latest", "http://169.254.170.2/v2", "http://169.254.0.23/",
                "http://100.100.100.200/latest", "http://[fd00:ec2::254]/latest")) {
            NotifyResult result = client(true).post(url, Map.of(), "{}");
            assertThat(result.success()).as(url).isFalse();
            assertThat(result.httpStatus()).as(url).isZero();
            assertThat(result.responseBody()).as(url).contains("受限地址");
        }
    }

    @Test
    void 内嵌IPv4的IPv6地址按内嵌地址判断() {
        for (String url : List.of("http://[::ffff:127.0.0.1]:" + port() + "/n", "http://[::7f00:1]:" + port() + "/n",
                "http://[64:ff9b::a00:1]/n")) {
            NotifyResult result = client(false).post(url, Map.of(), "{}");
            assertThat(result.success()).as(url).isFalse();
            assertThat(result.responseBody()).as(url).contains("受限地址");
        }
        for (String url : List.of("http://[::a9fe:a9fe]/n", "http://[64:ff9b::a9fe:a9fe]/n", "http://[::ffff:169.254.169.254]/n")) {
            assertThat(client(true).post(url, Map.of(), "{}").responseBody()).as(url).contains("受限地址");
        }
        assertThat(receiver.requests).isEmpty();
    }

    @Test
    void 对方缓慢输出时按整体期限中止() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Thread dripper = Thread.ofVirtual().start(() -> {
                try (Socket socket = server.accept(); OutputStream out = socket.getOutputStream()) {
                    out.write("HTTP/1.1 200 OK\r\nContent-Length: 100000\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                    // 约 10 秒输出 50 字节：每次间隔小于读取超时，只有整体期限能提前中止
                    for (int i = 0; i < 50; i++) {
                        out.write('x');
                        out.flush();
                        Thread.sleep(200);
                    }
                } catch (Exception ignored) {
                    // 客户端中止后写入失败
                }
            });
            OpenProperties.Notify notify = new OpenProperties.Notify();
            notify.setAllowPrivateAddress(true);
            notify.setConnectTimeout(Duration.ofSeconds(1));
            notify.setReadTimeout(Duration.ofSeconds(1));
            long start = System.nanoTime();
            NotifyResult result = new OpenNotifyClient(notify)
                    .post("http://127.0.0.1:" + server.getLocalPort() + "/n", Map.of(), "{}");
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
            assertThat(result.success()).isFalse();
            assertThat(result.responseBody()).contains("发送超时");
            dripper.interrupt();
        }
    }

    @Test
    void 只允许http与https() {
        assertThat(client(true).post("ftp://127.0.0.1/n", Map.of(), "{}").success()).isFalse();
    }

    @Test
    void 响应内容截断到2048字符() {
        receiver.responseBody = "x".repeat(5000);
        assertThat(client(true).post(receiver.url("/n"), Map.of(), "{}").responseBody()).hasSize(2048);
    }
}
