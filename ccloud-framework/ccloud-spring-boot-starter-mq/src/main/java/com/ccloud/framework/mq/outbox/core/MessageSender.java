package com.ccloud.framework.mq.outbox.core;

/**
 * 消息投递契约。默认实现走 Redis Stream，也可替换为 RocketMQ / Kafka 等。
 */
public interface MessageSender {

    /**
     * 投递消息。抛出异常表示投递失败，调用方会据此安排重试。
     */
    void send(String topic, String messageId, String payload);
}
