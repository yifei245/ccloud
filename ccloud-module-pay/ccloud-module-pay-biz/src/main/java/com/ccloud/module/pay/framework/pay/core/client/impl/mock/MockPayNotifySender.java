package com.ccloud.module.pay.framework.pay.core.client.impl.mock;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mock 回调发送器，仅在开启模拟支付时注册
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ccloud.pay", name = "mock-enabled", havingValue = "true")
public class MockPayNotifySender implements DisposableBean {

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "pay-mock-notify");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 异步发送回调，失败只记日志：真实渠道的回调同样可能丢失，由补查任务兜底
     */
    public void send(String url, String secret, String body, long delaySeconds) {
        executor.schedule(() -> {
            long ts = System.currentTimeMillis();
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .header(MockPayNotifySigner.HEADER_TIMESTAMP, String.valueOf(ts))
                        .header(MockPayNotifySigner.HEADER_SIGN, MockPayNotifySigner.sign(secret, ts, body))
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("[mock-notify] {} 返回 {}：{}", url, response.statusCode(), StrUtil.maxLength(response.body(), 200));
            } catch (Exception e) {
                log.warn("[mock-notify] 回调 {} 失败，等待补查", url, e);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }
}
