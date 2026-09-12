package com.ccloud.framework.mq.outbox.config;

import com.ccloud.framework.mq.outbox.core.*;
import com.ccloud.framework.mq.redis.core.RedisStreamMessageSender;
import com.ccloud.framework.mq.eventbus.DomainEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 本地消息表自动配置
 */
@AutoConfiguration
public class CcloudOutboxAutoConfiguration {

    /**
     * 仅在存在 Redis 时提供默认投递实现；
     * 业务侧也可注册自己的 MessageSender 覆盖它
     */
    @Bean
    @ConditionalOnMissingBean(MessageSender.class)
    @ConditionalOnBean(StringRedisTemplate.class)
    public MessageSender redisStreamMessageSender(StringRedisTemplate stringRedisTemplate) {
        return new RedisStreamMessageSender(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnBean(MessageSender.class)
    public OutboxPublisher outboxPublisher(MessageSender messageSender,
                                           OutboxMessageMapper outboxMessageMapper) {
        return new OutboxPublisher(messageSender, outboxMessageMapper);
    }

    @Bean
    @ConditionalOnBean(OutboxPublisher.class)
    public OutboxMessageService outboxMessageService(OutboxMessageMapper outboxMessageMapper,
                                                     OutboxPublisher outboxPublisher) {
        return new OutboxMessageService(outboxMessageMapper, outboxPublisher);
    }

    @Bean
    @ConditionalOnBean(OutboxPublisher.class)
    public OutboxRetryJob outboxRetryJob(OutboxMessageMapper outboxMessageMapper,
                                         OutboxPublisher outboxPublisher) {
        return new OutboxRetryJob(outboxMessageMapper, outboxPublisher);
    }

    @Bean
    public IdempotentConsumer idempotentConsumer(ConsumeLogMapper consumeLogMapper,
                                                 PlatformTransactionManager transactionManager) {
        return new IdempotentConsumer(consumeLogMapper, new TransactionTemplate(transactionManager));
    }

    @Bean
    public DomainEventPublisher domainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new DomainEventPublisher(applicationEventPublisher);
    }
}
