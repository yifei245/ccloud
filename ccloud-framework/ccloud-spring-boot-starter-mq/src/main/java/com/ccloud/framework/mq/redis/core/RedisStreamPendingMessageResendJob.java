package com.ccloud.framework.mq.redis.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import com.ccloud.framework.mq.outbox.core.DeadLetterDO;
import com.ccloud.framework.mq.outbox.core.DeadLetterMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ccloud.framework.mq.redis.core.RedisStreamMessageSender.FIELD_MESSAGE_ID;
import static com.ccloud.framework.mq.redis.core.RedisStreamMessageSender.FIELD_PAYLOAD;
import static com.ccloud.framework.mq.redis.core.RedisStreamMessageSender.FIELD_RETRY;

/**
 * 待处理消息重投任务：处理失败或消费者宕机而未 ACK、且闲置超过阈值的消息，复制一条新消息重新入队后 ACK 旧消息。
 *
 * <p>重新入队而非 XCLAIM：新消息对全组可见，任一存活实例都能处理；重投次数记在消息字段里，超过上限写入死信表（无数据库时仅记错误日志）后 ACK 丢弃。
 * 先入队再 ACK：两步之间宕机只会重复投递，不会丢消息。
 */
@Slf4j
public class RedisStreamPendingMessageResendJob {

    private static final int BATCH_SIZE = 100;

    private final StringRedisTemplate redisTemplate;
    private final String group;
    private final List<AbstractRedisStreamMessageListener<?>> listeners;
    private final Duration idleTimeout;
    private final int maxRetry;
    private final ObjectProvider<DeadLetterMapper> deadLetterMapper;

    /**
     * @param idleTimeout 消息投递后多久未 ACK 才重投，须大于正常处理耗时，否则处理中的消息会被重复投递
     * @param maxRetry    最大重投次数
     * @param deadLetterMapper 超过重投上限的消息写入死信表；不可用（无数据库的服务）时只记错误日志
     */
    public RedisStreamPendingMessageResendJob(StringRedisTemplate redisTemplate, String group,
                                              List<AbstractRedisStreamMessageListener<?>> listeners,
                                              Duration idleTimeout, int maxRetry,
                                              ObjectProvider<DeadLetterMapper> deadLetterMapper) {
        this.redisTemplate = redisTemplate;
        this.group = group;
        this.listeners = listeners;
        this.idleTimeout = idleTimeout;
        this.maxRetry = maxRetry;
        this.deadLetterMapper = deadLetterMapper;
    }

    /**
     * XXL-Job 处理器名：redisStreamPendingMessageResendJob
     *
     * @return 执行结果摘要
     */
    public String execute() {
        int resent = 0;
        int dropped = 0;
        StreamOperations<String, String, String> ops = redisTemplate.opsForStream();
        for (AbstractRedisStreamMessageListener<?> listener : listeners) {
            String streamKey = RedisStreamMessageSender.streamKey(listener.getTopic());
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(streamKey))) {
                continue;
            }
            PendingMessages pending = ops.pending(streamKey, group, Range.unbounded(), BATCH_SIZE);
            for (PendingMessage message : pending) {
                if (message.getElapsedTimeSinceLastDelivery().compareTo(idleTimeout) < 0) {
                    continue;
                }
                List<MapRecord<String, String, String>> records =
                        ops.range(streamKey, Range.just(message.getIdAsString()));
                if (CollUtil.isEmpty(records)) {
                    // 已被裁剪，无法重投
                    ops.acknowledge(streamKey, group, message.getId());
                    dropped++;
                    continue;
                }
                Map<String, String> fields = new HashMap<>(records.get(0).getValue());
                int retry = NumberUtil.parseInt(fields.get(FIELD_RETRY), 0);
                if (retry >= maxRetry) {
                    log.error("[execute] 主题 {} 消息 {} 重投 {} 次仍失败，丢弃", listener.getTopic(),
                            fields.get(FIELD_MESSAGE_ID), retry);
                    saveDeadLetter(listener.getTopic(), fields, retry);
                    ops.acknowledge(streamKey, group, message.getId());
                    dropped++;
                    continue;
                }
                fields.put(FIELD_RETRY, String.valueOf(retry + 1));
                RedisStreamMessageSender.add(redisTemplate, streamKey, fields);
                ops.acknowledge(streamKey, group, message.getId());
                resent++;
            }
        }
        String result = String.format("重投 %d 条，丢弃 %d 条", resent, dropped);
        if (resent + dropped > 0) {
            log.info("[execute] {}", result);
        }
        return result;
    }

    /**
     * 先落死信再 ACK：写库失败则抛出，消息仍留在待处理列表，下次再试，不会丢。
     * 没有数据库的服务拿不到 Mapper，维持只记日志。Stream 消息没有租户信息，记为 0
     */
    private void saveDeadLetter(String topic, Map<String, String> fields, int retry) {
        DeadLetterMapper mapper = deadLetterMapper.getIfAvailable();
        if (mapper == null) {
            return;
        }
        DeadLetterDO deadLetter = new DeadLetterDO();
        deadLetter.setMessageId(fields.get(FIELD_MESSAGE_ID));
        deadLetter.setTopic(topic);
        deadLetter.setPayload(fields.get(FIELD_PAYLOAD));
        deadLetter.setRetryCount(retry);
        deadLetter.setErrorMsg("消费重试 " + retry + " 次仍失败");
        deadLetter.setHandled(false);
        deadLetter.setTenantId(0L);
        mapper.insert(deadLetter);
    }
}
