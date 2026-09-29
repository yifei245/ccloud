package com.ccloud.framework.mq.redis.core;

import com.ccloud.framework.mq.outbox.core.MessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
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

    static final String FIELD_MESSAGE_ID = "messageId";
    static final String FIELD_PAYLOAD = "payload";
    /**
     * 已重投次数，由 {@link RedisStreamPendingMessageResendJob} 写入
     */
    static final String FIELD_RETRY = "retry";

    /**
     * 每个 Stream 保留的最大条数（近似裁剪）。已 ACK 的消息不会自动删除，不裁剪会无限增长；
     * 上限远大于正常积压量，只裁掉早已消费完的旧消息
     */
    static final long MAX_LENGTH = 100_000;

    private final StringRedisTemplate stringRedisTemplate;

    public static String streamKey(String topic) {
        return String.format(STREAM_KEY, topic);
    }

    @Override
    public void send(String topic, String messageId, String payload) {
        add(stringRedisTemplate, streamKey(topic), Map.of(FIELD_MESSAGE_ID, messageId, FIELD_PAYLOAD, payload));
    }

    static void add(StringRedisTemplate redisTemplate, String streamKey, Map<String, String> fields) {
        MapRecord<String, String, String> record = StreamRecords.newRecord().in(streamKey).ofMap(fields);
        redisTemplate.opsForStream().add(record, XAddOptions.maxlen(MAX_LENGTH).approximateTrimming(true));
    }
}
