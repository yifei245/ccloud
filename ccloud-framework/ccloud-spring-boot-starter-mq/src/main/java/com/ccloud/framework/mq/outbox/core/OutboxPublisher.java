package com.ccloud.framework.mq.outbox.core;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 消息投递器。负责调用 {@link MessageSender} 并把结果回写到本地消息表。
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private final MessageSender messageSender;
    private final OutboxMessageMapper outboxMessageMapper;
    private final DeadLetterMapper deadLetterMapper;

    /**
     * 投递一条消息，并按结果更新状态。
     *
     * <p>本方法<b>不抛异常</b>：投递失败属于预期内的情况，由重试机制兜底，
     * 不该把业务线程或调度线程打断。
     */
    public void publish(OutboxMessageDO message) {
        try {
            messageSender.send(message.getTopic(), message.getMessageId(), message.getPayload());
            markSuccess(message);
        } catch (Exception e) {
            log.warn("[publish] 消息 {} 投递失败，将安排重试", message.getMessageId(), e);
            markFailure(message, e);
        }
    }

    private void markSuccess(OutboxMessageDO message) {
        OutboxMessageDO update = new OutboxMessageDO();
        update.setId(message.getId());
        update.setStatus(OutboxStatusEnum.SUCCESS.getStatus());
        update.setNextRetryTime(null);
        outboxMessageMapper.updateById(update);
    }

    private void markFailure(OutboxMessageDO message, Exception e) {
        int retryCount = (message.getRetryCount() == null ? 0 : message.getRetryCount()) + 1;
        int maxRetry = message.getMaxRetry() == null ? 5 : message.getMaxRetry();

        OutboxMessageDO update = new OutboxMessageDO();
        update.setId(message.getId());
        update.setRetryCount(retryCount);
        update.setErrorMsg(StrUtil.maxLength(e.getMessage(), 900));
        if (retryCount >= maxRetry && saveDeadLetter(message, retryCount, update.getErrorMsg())) {
            // 超过上限转死信，避免无效重试永远占用调度资源
            update.setStatus(OutboxStatusEnum.DEAD.getStatus());
            log.error("[markFailure] 消息 {} 重试 {} 次仍失败，转入死信", message.getMessageId(), retryCount);
        } else {
            // 死信写入失败时同样走退避重试，下一轮会再次尝试转死信，不让消息既不重投也没有死信记录
            update.setStatus(OutboxStatusEnum.FAILURE.getStatus());
            // 指数退避：下游故障时，固定间隔重试会持续加压，让它更难恢复
            update.setNextRetryTime(LocalDateTime.now().plusMinutes(1L << Math.min(retryCount - 1, 10)));
        }
        outboxMessageMapper.updateById(update);
    }

    /**
     * 死信的租户取自原消息。刚插入的消息对象上租户编号为空（拦截器只改写 SQL，不回填实体），此时回读库中记录
     */
    private boolean saveDeadLetter(OutboxMessageDO message, int retryCount, String errorMsg) {
        try {
            doSaveDeadLetter(message, retryCount, errorMsg);
            return true;
        } catch (Exception e) {
            log.error("[saveDeadLetter] 消息 {} 写入死信表失败，保持待重试", message.getMessageId(), e);
            return false;
        }
    }

    private void doSaveDeadLetter(OutboxMessageDO message, int retryCount, String errorMsg) {
        Long tenantId = message.getTenantId();
        if (tenantId == null) {
            OutboxMessageDO stored = outboxMessageMapper.selectById(message.getId());
            tenantId = stored == null ? null : stored.getTenantId();
        }
        DeadLetterDO deadLetter = new DeadLetterDO();
        deadLetter.setMessageId(message.getMessageId());
        deadLetter.setTopic(message.getTopic());
        deadLetter.setPayload(message.getPayload());
        deadLetter.setRetryCount(retryCount);
        deadLetter.setErrorMsg(errorMsg);
        deadLetter.setHandled(false);
        deadLetter.setTenantId(tenantId == null ? 0L : tenantId);
        deadLetterMapper.insert(deadLetter.truncateToColumnWidth());
    }
}
