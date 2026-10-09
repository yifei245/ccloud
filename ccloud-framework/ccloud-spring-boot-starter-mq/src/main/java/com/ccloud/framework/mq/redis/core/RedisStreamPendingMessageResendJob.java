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
        int failed = 0;
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
                // 单条失败（如写死信失败）只跳过该条且不 ACK，下一轮再试；不能中断整轮，
                // 否则它每轮都排在队首，后面的消息与其他主题永远得不到处理
                String messageId = null;
                try {
                    List<MapRecord<String, String, String>> records =
                            ops.range(streamKey, Range.just(message.getIdAsString()));
                    Map<String, String> fields = CollUtil.isEmpty(records) ? null : new HashMap<>(records.get(0).getValue());
                    messageId = fields == null ? null : fields.get(FIELD_MESSAGE_ID);
                    if (handle(ops, streamKey, listener.getTopic(), message, fields)) {
                        resent++;
                    } else {
                        dropped++;
                    }
                } catch (Exception e) {
                    failed++;
                    log.error("[execute] 主题 {} 消息 {}（Stream 记录 {}）处理失败，不 ACK，留待下一轮",
                            listener.getTopic(), messageId, message.getIdAsString(), e);
                }
            }
        }
        String result = String.format("重投 %d 条，丢弃 %d 条", resent, dropped);
        if (failed > 0) {
            result += String.format("，失败 %d 条", failed);
        }
        if (resent + dropped + failed > 0) {
            log.info("[execute] {}", result);
        }
        return result;
    }

    /**
     * @param fields 消息字段；为空表示消息已被裁剪
     * @return true 重投；false 丢弃（已裁剪或超过重投上限）
     */
    private boolean handle(StreamOperations<String, String, String> ops, String streamKey, String topic,
                           PendingMessage message, Map<String, String> fields) {
        if (fields == null) {
            // 已被裁剪，无法重投
            ops.acknowledge(streamKey, group, message.getId());
            return false;
        }
        int retry = NumberUtil.parseInt(fields.get(FIELD_RETRY), 0);
        if (retry >= maxRetry) {
            saveDeadLetter(topic, fields, retry);
            ops.acknowledge(streamKey, group, message.getId());
            log.error("[handle] 主题 {} 消息 {} 重投 {} 次仍失败，转入死信", topic, fields.get(FIELD_MESSAGE_ID), retry);
            return false;
        }
        fields.put(FIELD_RETRY, String.valueOf(retry + 1));
        RedisStreamMessageSender.add(redisTemplate, streamKey, fields);
        ops.acknowledge(streamKey, group, message.getId());
        return true;
    }

    /**
     * 先落死信再 ACK：写库失败则抛出，由调用方跳过该条，消息仍留在待处理列表，下次再试，不会丢。
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
        mapper.insert(deadLetter.truncateToColumnWidth());
    }
}
