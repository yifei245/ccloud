package com.ccloud.framework.mq.redis.core;

import lombok.Getter;

/**
 * Redis Stream 消息监听器。声明为 Spring Bean 即自动订阅，同一服务的多个实例同属一个消费组，每条消息只由其中一个处理。
 *
 * <p>投递语义为<b>至少一次</b>：处理抛异常时消息不 ACK，由 {@link RedisStreamPendingMessageResendJob} 重投。
 * 实现必须幂等，可借助 {@link com.ccloud.framework.mq.outbox.core.IdempotentConsumer} 或业务状态判断。
 *
 * @param <T> 消息体类型，由 JSON 反序列化
 */
@Getter
public abstract class AbstractRedisStreamMessageListener<T> {

    private final String topic;
    private final Class<T> messageType;

    protected AbstractRedisStreamMessageListener(String topic, Class<T> messageType) {
        this.topic = topic;
        this.messageType = messageType;
    }

    /**
     * @param messageId 发送方生成的消息编号，重投时保持不变，可用于幂等
     */
    public abstract void onMessage(String messageId, T message);
}
