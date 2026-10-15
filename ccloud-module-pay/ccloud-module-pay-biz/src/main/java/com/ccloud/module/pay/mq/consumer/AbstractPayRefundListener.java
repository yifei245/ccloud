package com.ccloud.module.pay.mq.consumer;

import com.ccloud.framework.mq.outbox.core.IdempotentConsumer;
import com.ccloud.framework.mq.redis.core.AbstractRedisStreamMessageListener;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.enums.refund.PayRefundStatusEnum;
import com.ccloud.module.pay.mq.message.PayRefundMessage;
import com.ccloud.module.pay.service.notify.PayOpenNotifyService;
import com.ccloud.module.pay.service.wallet.PayWalletRechargeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.ccloud.module.pay.mq.consumer.PayConsumers.CONSUMER_OPEN_NOTIFY;
import static com.ccloud.module.pay.mq.consumer.PayConsumers.CONSUMER_RECHARGE;
import static com.ccloud.module.pay.mq.consumer.PayConsumers.toMillis;

/**
 * 退款结果：钱包充值的退款先处理充值（成功记退款成功，失败退回扣回的金额），再转发给绑定了开放应用的外部系统。
 * 两件事各自幂等。成功与失败各订阅一个主题，由子类提供主题、开放平台事件名与该主题应有的状态。
 * 消息的状态不是该主题应有的状态（空、非终态、未知，或与主题不符）说明发送方有缺陷：抛异常，
 * 让消息重投并最终进入死信，两件事都不执行，不静默确认
 */
@Slf4j
public abstract class AbstractPayRefundListener extends AbstractRedisStreamMessageListener<PayRefundMessage> {

    private final String event;
    private final PayRefundStatusEnum expectedStatus;

    @Resource
    private IdempotentConsumer idempotentConsumer;
    @Resource
    private PayWalletRechargeService rechargeService;
    @Resource
    private PayOpenNotifyService openNotifyService;

    protected AbstractPayRefundListener(String topic, String event, PayRefundStatusEnum expectedStatus) {
        super(topic, PayRefundMessage.class);
        this.event = event;
        this.expectedStatus = expectedStatus;
    }

    @Override
    public void onMessage(String messageId, PayRefundMessage message) {
        PayConsumers.requireTenant(messageId, getTopic(), message.getTenantId());
        if (!expectedStatus.getStatus().equals(message.getStatus())) {
            log.error("[onMessage] 退款消息 {}（主题 {}，退款单 {}）状态 {} 与主题应有的状态 {} 不符，拒绝处理",
                    messageId, getTopic(), message.getRefundNo(), message.getStatus(), expectedStatus.getStatus());
            throw new IllegalStateException("退款消息 " + messageId + "（主题 " + getTopic() + "）状态 " + message.getStatus()
                    + " 与主题应有的状态 " + expectedStatus.getStatus() + " 不符");
        }
        TenantUtils.execute(message.getTenantId(), () -> {
            if (rechargeService.isRechargeApp(message.getAppId())) {
                idempotentConsumer.consumeOnce(messageId, CONSUMER_RECHARGE, () -> rechargeService.handleRechargeRefunded(message));
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("merchantOrderId", message.getMerchantOrderId());
            data.put("merchantRefundId", message.getMerchantRefundId());
            data.put("refundNo", message.getRefundNo());
            data.put("refundPrice", message.getRefundPrice());
            data.put("status", expectedStatus.name());
            data.put("successTime", toMillis(message.getSuccessTime()));
            data.put("errorMsg", message.getChannelErrorMsg());
            idempotentConsumer.consumeOnce(messageId, CONSUMER_OPEN_NOTIFY,
                    () -> openNotifyService.forward(messageId, event, message.getAppId(), data));
        });
    }
}
