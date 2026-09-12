package com.ccloud.framework.mq.outbox.core;

import cn.hutool.core.util.IdUtil;
import com.ccloud.framework.common.util.json.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * 本地消息表的写入入口。
 *
 * <p>用法：在业务的 {@code @Transactional} 方法内调用 {@link #saveAndPublish}。
 * 消息与业务数据写在同一个事务里，投递则推迟到事务提交之后。
 *
 * <p>推迟投递是关键：若在提交前投递，下游收到消息后回查业务数据会读不到
 * （事务尚未提交），或者更糟——业务随后回滚，而消息已经发出去了。
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxMessageService {

    private final OutboxMessageMapper outboxMessageMapper;
    private final OutboxPublisher outboxPublisher;

    public String saveAndPublish(String topic, Object payload) {
        return saveAndPublish(topic, payload, 5);
    }

    /**
     * @param maxRetry 最大重试次数，超过后转死信
     * @return 消息编号，消费侧用它做幂等去重
     */
    public String saveAndPublish(String topic, Object payload, int maxRetry) {
        OutboxMessageDO message = new OutboxMessageDO();
        message.setMessageId(IdUtil.fastSimpleUUID());
        message.setTopic(topic);
        message.setPayload(JsonUtils.toJsonString(payload));
        message.setStatus(OutboxStatusEnum.PENDING.getStatus());
        message.setRetryCount(0);
        message.setMaxRetry(maxRetry);
        // 立刻可被重试任务捞起：万一 afterCommit 投递因进程崩溃没执行，
        // 定时任务能接手，不会有消息永远停在待投递状态
        message.setNextRetryTime(LocalDateTime.now());
        outboxMessageMapper.insert(message);

        publishAfterCommit(message);
        return message.getMessageId();
    }

    private void publishAfterCommit(OutboxMessageDO message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 没有事务时直接投递。这种用法失去了"与业务同生共死"的保证，
            // 仅适用于本就不在事务中的场景
            log.debug("[publishAfterCommit] 当前无事务，消息 {} 直接投递", message.getMessageId());
            outboxPublisher.publish(message);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                outboxPublisher.publish(message);
            }
        });
    }
}
