package com.ccloud.gateway.service.open;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 订阅 open 发布的应用缓存失效消息。频道须与 open 的 OpenAppCacheConstants.INVALIDATE_CHANNEL 逐字一致；
 * 消息体为 appKey，空串表示全部失效。
 *
 * <p>订阅出错（启动时 Redis 不可用、连接中断）会无限退避重试，每次重试都新建 container——
 * 订阅连接是懒连接且会缓存失败，复用旧 container 无法恢复。每次（重新）订阅成功后清空全部缓存，
 * 补上断线期间可能丢失的消息；在订阅成功之后才清空，不留空窗。重试期间只能靠缓存 TTL 兜底
 */
@Slf4j
@Component
public class OpenAppInvalidationListener implements SmartLifecycle {

    public static final String CHANNEL = "ccloud:open:app:invalidate";

    private static final Duration MAX_BACKOFF = Duration.ofSeconds(30);

    private final OpenAppService openAppService;
    /**
     * 每次调用建立一次新的订阅：订阅成功时发出消息流，失败时发出错误
     */
    private final Supplier<Mono<Flux<String>>> subscriber;
    private final Duration minBackoff;
    private volatile ReactiveRedisMessageListenerContainer container;
    private Disposable subscription;

    @Autowired
    public OpenAppInvalidationListener(ReactiveRedisConnectionFactory connectionFactory,
                                       OpenAppService openAppService) {
        this.openAppService = openAppService;
        this.subscriber = () -> Mono.defer(() -> {
            destroyContainer();
            container = new ReactiveRedisMessageListenerContainer(connectionFactory);
            return container.receiveLater(ChannelTopic.of(CHANNEL))
                    .map(messages -> messages.map(ReactiveSubscription.Message::getMessage));
        });
        this.minBackoff = Duration.ofSeconds(1);
    }

    /**
     * 供测试替换订阅来源与退避时长
     */
    OpenAppInvalidationListener(OpenAppService openAppService, Supplier<Mono<Flux<String>>> subscriber,
                                Duration minBackoff) {
        this.openAppService = openAppService;
        this.subscriber = subscriber;
        this.minBackoff = minBackoff;
    }

    @Override
    public synchronized void start() {
        if (subscription != null && !subscription.isDisposed()) {
            return;
        }
        subscription = Flux.defer(() -> subscriber.get().flatMapMany(messages -> {
                    log.info("[start] 已订阅应用缓存失效频道 {}", CHANNEL);
                    openAppService.invalidateAll();
                    // 消息流意外结束也按失败处理，走重试重新订阅
                    return messages.concatWith(Mono.error(new IllegalStateException("应用缓存失效订阅意外结束")));
                }))
                .doOnNext(this::handle)
                .retryWhen(Retry.backoff(Long.MAX_VALUE, minBackoff).maxBackoff(MAX_BACKOFF).transientErrors(true)
                        .doBeforeRetry(signal -> log.warn("[start] 应用缓存失效订阅中断，稍后重试，期间依赖缓存 TTL 兜底",
                                signal.failure())))
                .subscribe(v -> { }, e -> log.error("[start] 应用缓存失效订阅终止", e));
    }

    private void handle(String appKey) {
        try {
            if (appKey == null || appKey.isEmpty()) {
                openAppService.invalidateAll();
            } else {
                openAppService.invalidate(appKey);
            }
        } catch (RuntimeException e) {
            log.warn("[handle] 处理应用缓存失效消息失败 appKey={}", appKey, e);
        }
    }

    @Override
    public synchronized void stop() {
        if (subscription != null) {
            subscription.dispose();
        }
        destroyContainer();
    }

    private void destroyContainer() {
        if (container != null) {
            container.destroyLater().subscribe(v -> { }, e -> log.warn("[destroyContainer] 销毁订阅容器失败", e));
            container = null;
        }
    }

    @Override
    public boolean isRunning() {
        return subscription != null && !subscription.isDisposed();
    }
}
