package com.ccloud.gateway.service.open;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

/**
 * 订阅 open 发布的应用缓存失效消息。频道须与 open 的 OpenAppCacheConstants.INVALIDATE_CHANNEL 逐字一致；
 * 消息体为 appKey，空串表示全部失效。消息丢失时最多延迟一个缓存周期
 */
@Slf4j
@Component
public class OpenAppInvalidationListener implements SmartLifecycle {

    public static final String CHANNEL = "ccloud:open:app:invalidate";

    private final ReactiveRedisConnectionFactory connectionFactory;
    private final OpenAppService openAppService;
    private ReactiveRedisMessageListenerContainer container;
    private Disposable subscription;

    public OpenAppInvalidationListener(ReactiveRedisConnectionFactory connectionFactory,
                                       OpenAppService openAppService) {
        this.connectionFactory = connectionFactory;
        this.openAppService = openAppService;
    }

    @Override
    public void start() {
        container = new ReactiveRedisMessageListenerContainer(connectionFactory);
        subscription = container.receive(ChannelTopic.of(CHANNEL))
                .map(ReactiveSubscription.Message::getMessage)
                .doOnNext(appKey -> {
                    if (appKey == null || appKey.isEmpty()) {
                        openAppService.invalidateAll();
                    } else {
                        openAppService.invalidate(appKey);
                    }
                })
                .onErrorContinue((e, v) -> log.warn("[start] 处理应用缓存失效消息失败", e))
                .subscribe();
    }

    @Override
    public void stop() {
        if (subscription != null) {
            subscription.dispose();
        }
        if (container != null) {
            container.destroyLater().subscribe();
        }
    }

    @Override
    public boolean isRunning() {
        return subscription != null && !subscription.isDisposed();
    }
}
