package com.ccloud.framework.mq.eventbus;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 进程内领域事件。
 *
 * <p>与本地消息表的分工：同进程内的解耦用它（例如用户创建后初始化默认配置），
 * 跨服务的最终一致性用 {@code OutboxMessageService}。
 * 领域事件不保证投递，进程崩溃即丢失。
 */
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 在当前事务提交后发布。避免监听方读不到尚未提交的数据。
     */
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}
