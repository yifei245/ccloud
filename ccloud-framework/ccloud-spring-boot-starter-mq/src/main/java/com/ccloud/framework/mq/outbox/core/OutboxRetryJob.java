package com.ccloud.framework.mq.outbox.core;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息重投任务。
 *
 * <p>它是整个方案的兜底：应用在 afterCommit 投递前崩溃、投递时下游不可用，
 * 消息都会停在待投递或失败状态，靠它捞回来重投。
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxRetryJob {

    private static final int BATCH_SIZE = 100;

    private final OutboxMessageMapper outboxMessageMapper;
    private final OutboxPublisher outboxPublisher;

    /**
     * XXL-Job 处理器名：outboxRetryJob
     *
     * @return 执行结果摘要
     */
    public String execute() {
        List<OutboxMessageDO> messages =
                outboxMessageMapper.selectRetryable(LocalDateTime.now(), BATCH_SIZE);
        if (CollUtil.isEmpty(messages)) {
            return "无待重投消息";
        }
        int success = 0;
        for (OutboxMessageDO message : messages) {
            outboxPublisher.publish(message);
            // publish 不抛异常，据状态判断本次是否成功
            OutboxMessageDO latest = outboxMessageMapper.selectById(message.getId());
            if (latest != null && OutboxStatusEnum.SUCCESS.getStatus().equals(latest.getStatus())) {
                success++;
            }
        }
        String result = String.format("重投 %d 条，成功 %d 条", messages.size(), success);
        log.info("[execute] {}", result);
        return result;
    }
}
