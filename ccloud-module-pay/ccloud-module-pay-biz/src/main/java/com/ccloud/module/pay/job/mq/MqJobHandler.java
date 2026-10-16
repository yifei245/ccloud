package com.ccloud.module.pay.job.mq;

import com.ccloud.framework.mq.redis.core.RedisStreamPendingMessageResendJob;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * pay 消费组（支付、退款、转账消息）的未确认消息重投，需在 XXL-Job 为本服务登记（每分钟执行）
 */
@Component
public class MqJobHandler {

    private final ObjectProvider<RedisStreamPendingMessageResendJob> pendingMessageResendJob;

    public MqJobHandler(ObjectProvider<RedisStreamPendingMessageResendJob> pendingMessageResendJob) {
        this.pendingMessageResendJob = pendingMessageResendJob;
    }

    @XxlJob("redisStreamPendingMessageResendJob")
    public void redisStreamPendingMessageResend() {
        RedisStreamPendingMessageResendJob job = pendingMessageResendJob.getIfAvailable();
        XxlJobHelper.handleSuccess(job == null ? "无 Redis Stream 监听器" : job.execute());
    }
}
