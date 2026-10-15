package com.ccloud.module.pay.mq.consumer;

import com.ccloud.framework.mq.outbox.core.IdempotentConsumer;
import com.ccloud.framework.mq.redis.core.AbstractRedisStreamMessageListener;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.enums.transfer.PayTransferStatusEnum;
import com.ccloud.module.pay.mq.message.PayTransferMessage;
import com.ccloud.module.pay.service.notify.PayOpenNotifyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.ccloud.module.pay.mq.consumer.PayConsumers.CONSUMER_OPEN_NOTIFY;
import static com.ccloud.module.pay.mq.consumer.PayConsumers.toMillis;

/**
 * 转账结果：转发给绑定了开放应用的外部系统。成功与失败各订阅一个主题，由子类提供主题、开放平台事件名与该主题应有的状态。
 * 消息的状态不是该主题应有的状态（空、非终态、未知，或与主题不符）说明发送方有缺陷：抛异常，
 * 让消息重投并最终进入死信，不转发，不静默确认
 */
@Slf4j
public abstract class AbstractPayTransferListener extends AbstractRedisStreamMessageListener<PayTransferMessage> {

    private final String event;
    private final PayTransferStatusEnum expectedStatus;

    @Resource
    private IdempotentConsumer idempotentConsumer;
    @Resource
    private PayOpenNotifyService openNotifyService;

    protected AbstractPayTransferListener(String topic, String event, PayTransferStatusEnum expectedStatus) {
        super(topic, PayTransferMessage.class);
        this.event = event;
        this.expectedStatus = expectedStatus;
    }

    @Override
    public void onMessage(String messageId, PayTransferMessage message) {
        PayConsumers.requireTenant(messageId, getTopic(), message.getTenantId());
        if (!expectedStatus.getStatus().equals(message.getStatus())) {
            log.error("[onMessage] 转账消息 {}（主题 {}，转账单 {}）状态 {} 与主题应有的状态 {} 不符，拒绝处理",
                    messageId, getTopic(), message.getTransferNo(), message.getStatus(), expectedStatus.getStatus());
            throw new IllegalStateException("转账消息 " + messageId + "（主题 " + getTopic() + "）状态 " + message.getStatus()
                    + " 与主题应有的状态 " + expectedStatus.getStatus() + " 不符");
        }
        TenantUtils.execute(message.getTenantId(), () -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("merchantTransferId", message.getMerchantTransferId());
            data.put("transferNo", message.getTransferNo());
            data.put("price", message.getPrice());
            data.put("status", expectedStatus.name());
            data.put("successTime", toMillis(message.getSuccessTime()));
            data.put("errorMsg", message.getChannelErrorMsg());
            idempotentConsumer.consumeOnce(messageId, CONSUMER_OPEN_NOTIFY,
                    () -> openNotifyService.forward(messageId, event, message.getAppId(), data));
        });
    }
}
