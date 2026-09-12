package com.ccloud.framework.mq.redis.core;

import com.ccloud.framework.mq.outbox.core.MessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

/**
 * 基于 Redis Stream 的投递实现。
 *
 * <p>选 Stream 而非 Pub/Sub：Pub/Sub 不持久化，消费者不在线时消息直接丢失，
 * 与"最终一致性"的目标相悖。Stream 有消费组与 ACK，消费者重启后能继续处理。
 */
@RequiredArgsConstructor
public class RedisStreamMessageSender implements MessageSender {

    private static final String STREAM_KEY = "ccloud:stream:%s";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void send(String topic, String messageId, String payload) {
        ObjectRecord<String, Map<String, String>> record = StreamRecords.newRecord()
                .ofObject(Map.of("messageId", messageId, "payload", payload))
                .withStreamKey(String.format(STREAM_KEY, topic));
        stringRedisTemplate.opsForStream().add(record);
    }
}
