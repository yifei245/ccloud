package com.ccloud.module.system.job.mq;

import com.ccloud.framework.mq.outbox.core.OutboxRetryJob;
import com.ccloud.framework.mq.redis.core.RedisStreamPendingMessageResendJob;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 消息可靠投递的两个兜底任务，需在 XXL-Job 控制台为本服务登记（建议每分钟执行）：
 * <ul>
 *   <li>outboxRetryJob：本地消息表中待投递、投递失败的消息重投到 Redis Stream；</li>
 *   <li>redisStreamPendingMessageResendJob：已投递但消费失败（未 ACK）的消息重新入队。</li>
 * </ul>
 */
@Component
public class MqJobHandler {

    private final ObjectProvider<OutboxRetryJob> outboxRetryJob;
    private final ObjectProvider<RedisStreamPendingMessageResendJob> pendingMessageResendJob;

    public MqJobHandler(ObjectProvider<OutboxRetryJob> outboxRetryJob,
                        ObjectProvider<RedisStreamPendingMessageResendJob> pendingMessageResendJob) {
        this.outboxRetryJob = outboxRetryJob;
        this.pendingMessageResendJob = pendingMessageResendJob;
    }

    /**
     * 本地消息表按租户隔离，重投要覆盖全部租户的消息
     */
    @XxlJob("outboxRetryJob")
    public void outboxRetry() {
        OutboxRetryJob job = outboxRetryJob.getIfAvailable();
        XxlJobHelper.handleSuccess(job == null ? "未启用本地消息表" : TenantUtils.executeIgnore(job::execute));
    }

    @XxlJob("redisStreamPendingMessageResendJob")
    public void redisStreamPendingMessageResend() {
        RedisStreamPendingMessageResendJob job = pendingMessageResendJob.getIfAvailable();
        XxlJobHelper.handleSuccess(job == null ? "无 Redis Stream 监听器" : job.execute());
    }
}
