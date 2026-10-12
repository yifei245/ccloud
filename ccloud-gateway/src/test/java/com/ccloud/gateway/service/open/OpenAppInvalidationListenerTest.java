package com.ccloud.gateway.service.open;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 失效订阅：出错后自动重试并恢复，（重新）订阅成功后清空全部缓存补上断线期间的消息
 */
class OpenAppInvalidationListenerTest {

    private final OpenAppService service = mock(OpenAppService.class);
    private OpenAppInvalidationListener listener;

    @AfterEach
    void tearDown() {
        if (listener != null) {
            listener.stop();
        }
    }

    @Test
    void 首次订阅失败后自动恢复_恢复后收到失效消息且订阅成功时清空全部缓存() {
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        AtomicInteger attempts = new AtomicInteger();
        listener = new OpenAppInvalidationListener(service, () -> attempts.incrementAndGet() <= 2
                ? Mono.error(new IllegalStateException("redis down"))
                : Mono.just(sink.asFlux()), Duration.ofMillis(50));

        listener.start();

        // 前两次失败，第三次订阅成功后清空一次全部缓存
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(service, times(1)).invalidateAll());
        assertThat(attempts.get()).isEqualTo(3);
        assertThat(listener.isRunning()).isTrue();

        sink.tryEmitNext("app-1");
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> verify(service).invalidate("app-1"));
        sink.tryEmitNext("");
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> verify(service, times(2)).invalidateAll());
    }

    @Test
    void 连接中断后重新订阅_并补一次全部失效() {
        Sinks.Many<String> first = Sinks.many().multicast().onBackpressureBuffer();
        Sinks.Many<String> second = Sinks.many().multicast().onBackpressureBuffer();
        AtomicInteger attempts = new AtomicInteger();
        listener = new OpenAppInvalidationListener(service, () -> Mono.just(
                attempts.incrementAndGet() == 1 ? first.asFlux() : second.asFlux()), Duration.ofMillis(50));

        listener.start();
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> verify(service, times(1)).invalidateAll());

        first.tryEmitError(new IllegalStateException("connection lost"));
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(service, times(2)).invalidateAll());
        second.tryEmitNext("app-2");
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> verify(service).invalidate("app-2"));
    }

    @Test
    void 消息流意外结束也会重新订阅() {
        AtomicInteger attempts = new AtomicInteger();
        listener = new OpenAppInvalidationListener(service, () -> {
            attempts.incrementAndGet();
            return Mono.just(Flux.<String>empty());
        }, Duration.ofMillis(50));

        listener.start();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(attempts.get()).isGreaterThanOrEqualTo(2));
    }

    @Test
    void 处理消息抛异常不会中断订阅() {
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        org.mockito.Mockito.doThrow(new IllegalStateException("boom")).when(service).invalidate("bad");
        listener = new OpenAppInvalidationListener(service, () -> Mono.just(sink.asFlux()), Duration.ofMillis(50));
        listener.start();
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> verify(service).invalidateAll());

        sink.tryEmitNext("bad");
        sink.tryEmitNext("good");

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> verify(service).invalidate("good"));
        // 仍是同一次订阅，没有触发重新订阅后的 invalidateAll
        verify(service, times(1)).invalidateAll();
    }

    @Test
    void stop后不再处理消息() {
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        listener = new OpenAppInvalidationListener(service, () -> Mono.just(sink.asFlux()), Duration.ofMillis(50));
        listener.start();
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> verify(service).invalidateAll());

        listener.stop();
        sink.tryEmitNext("late");

        assertThat(listener.isRunning()).isFalse();
        verify(service, never()).invalidate(anyString());
    }
}
