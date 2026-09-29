package com.ccloud.framework.mq.redis.core;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.ccloud.framework.mq.redis.core.RedisStreamMessageSender.FIELD_MESSAGE_ID;
import static com.ccloud.framework.mq.redis.core.RedisStreamMessageSender.FIELD_PAYLOAD;

/**
 * 按消费组订阅全部监听器的 Stream，处理成功后 ACK。
 *
 * <p>消费组名取服务名：同一服务的多个实例分摊消息，不同服务各自完整消费。
 * 消费者名取 pid@host，保证实例间不重名；实例宕机后其未 ACK 的消息由重投任务按消费组统一捞回。
 */
@Slf4j
public class RedisStreamMessageConsumer implements SmartLifecycle {

    private final StringRedisTemplate redisTemplate;
    private final String group;
    private final List<AbstractRedisStreamMessageListener<?>> listeners;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private volatile boolean running;

    public RedisStreamMessageConsumer(StringRedisTemplate redisTemplate, String group,
                                      List<AbstractRedisStreamMessageListener<?>> listeners) {
        this.redisTemplate = redisTemplate;
        this.group = group;
        this.listeners = listeners;
    }

    @Override
    public void start() {
        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .batchSize(10)
                        .pollTimeout(Duration.ofSeconds(1))
                        .serializer(RedisSerializer.string())
                        .errorHandler(e -> log.error("[start] Redis Stream 拉取消息异常", e))
                        .build();
        container = StreamMessageListenerContainer.create(redisTemplate.getRequiredConnectionFactory(), options);
        Consumer consumer = Consumer.from(group, ManagementFactory.getRuntimeMXBean().getName());
        for (AbstractRedisStreamMessageListener<?> listener : listeners) {
            String streamKey = RedisStreamMessageSender.streamKey(listener.getTopic());
            createGroupIfAbsent(streamKey);
            // lastConsumed 即 ">"：只读取本组尚未投递过的消息；已投递未 ACK 的由重投任务处理
            container.receive(consumer, StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                    record -> dispatch(listener, record));
        }
        container.start();
        running = true;
        log.info("[start] 消费组 {} 订阅 {} 个主题", group, listeners.size());
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 从 0 开始建组：消费者首次启动前已发出的消息也要处理
     */
    private void createGroupIfAbsent(String streamKey) {
        try {
            redisTemplate.execute(connection -> connection.streamCommands().xGroupCreate(
                    RedisSerializer.string().serialize(streamKey), group, ReadOffset.from("0"), true), true);
        } catch (DataAccessException e) {
            // 消费组已存在（服务重启）。不同客户端包装方式不同：Lettuce 抛 RedisSystemException，
            // Redisson 抛 InvalidDataAccessApiUsageException，只能按原因链中的 BUSYGROUP 判断
            if (!isBusyGroup(e)) {
                throw e;
            }
        }
    }

    private static boolean isBusyGroup(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
            if (StrUtil.contains(cause.getMessage(), "BUSYGROUP")) {
                return true;
            }
        }
        return false;
    }

    private <T> void dispatch(AbstractRedisStreamMessageListener<T> listener, MapRecord<String, String, String> record) {
        Map<String, String> fields = record.getValue();
        String messageId = fields.get(FIELD_MESSAGE_ID);
        T message;
        try {
            message = JsonUtils.parseObject(fields.get(FIELD_PAYLOAD), listener.getMessageType());
        } catch (RuntimeException e) {
            // 消息体损坏重投也无法处理，ACK 掉，否则会一直留在待处理列表里反复重投
            log.error("[dispatch] 主题 {} 消息 {} 无法解析，丢弃：{}", listener.getTopic(), messageId, fields, e);
            acknowledge(record);
            return;
        }
        try {
            listener.onMessage(messageId, message);
        } catch (RuntimeException e) {
            log.warn("[dispatch] 主题 {} 消息 {} 处理失败，等待重投", listener.getTopic(), messageId, e);
            return;
        }
        acknowledge(record);
    }

    private void acknowledge(MapRecord<String, String, String> record) {
        redisTemplate.opsForStream().acknowledge(record.getStream(), group, record.getId());
    }
}
