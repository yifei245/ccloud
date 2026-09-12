package com.ccloud.framework.mq.outbox.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/**
 * 幂等消费。
 *
 * <p>实现要点：<b>先写消费日志，再执行业务动作，两者同一个事务</b>。
 *
 * <ul>
 *   <li>先写日志：靠 {@code (message_id, consumer)} 的唯一索引挡住并发重复投递。
 *       若改成"先执行再记日志"，两个并发投递会双双通过检查各执行一次；</li>
 *   <li>同一事务：业务动作失败时消费日志一并回滚，这条消息才能被重新投递。
 *       若日志独立提交，一次失败就会让消息永远消费不了。</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class IdempotentConsumer {

    private final ConsumeLogMapper consumeLogMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * 保证同一 {@code (messageId, consumer)} 的业务动作只执行一次
     *
     * @return true 表示本次真正执行了；false 表示此前已消费过，本次跳过
     */
    public boolean consumeOnce(String messageId, String consumer, Runnable action) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                ConsumeLogDO log = new ConsumeLogDO();
                log.setMessageId(messageId);
                log.setConsumer(consumer);
                log.setConsumeTime(LocalDateTime.now());
                consumeLogMapper.insert(log);
            } catch (DuplicateKeyException e) {
                // 已消费过。这是正常情况而非错误：消息中间件的投递语义就是至少一次
                IdempotentConsumer.log.debug("[consumeOnce] 消息 {} 已被 {} 消费过，跳过", messageId, consumer);
                return false;
            }
            action.run();
            return true;
        }));
    }
}
