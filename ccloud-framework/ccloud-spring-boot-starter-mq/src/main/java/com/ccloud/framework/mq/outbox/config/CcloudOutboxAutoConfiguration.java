package com.ccloud.framework.mq.outbox.config;

import com.ccloud.framework.mq.outbox.core.*;
import com.ccloud.framework.mq.redis.core.RedisStreamMessageSender;
import com.ccloud.framework.mq.eventbus.DomainEventPublisher;
import org.mybatis.spring.annotation.MapperScan;
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
 *
 * <p>本 starter 自带的 Mapper 由这里注册：业务服务的 MapperScan 只覆盖 com.ccloud.module.*，
 * 扫不到框架包。限定 @Mapper 注解，避免把 MessageSender 等普通接口也当成 Mapper 代理。
 */
// 投递实现以 StringRedisTemplate 为条件，必须排在 Redis 自动配置之后，否则条件判断时 Bean 尚未注册，投递能力被静默跳过
@AutoConfiguration(afterName = {"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "com.ccloud.framework.redis.config.CcloudRedisAutoConfiguration"})
@MapperScan(value = "com.ccloud.framework.mq.outbox.core",
        annotationClass = org.apache.ibatis.annotations.Mapper.class)
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
                                           OutboxMessageMapper outboxMessageMapper,
                                           DeadLetterMapper deadLetterMapper) {
        return new OutboxPublisher(messageSender, outboxMessageMapper, deadLetterMapper);
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
