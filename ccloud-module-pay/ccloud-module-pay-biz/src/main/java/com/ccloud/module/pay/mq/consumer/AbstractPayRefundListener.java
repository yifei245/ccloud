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

import static com.ccloud.module.pay.mq.consumer.PayOrderPaidListener.CONSUMER_OPEN_NOTIFY;
import static com.ccloud.module.pay.mq.consumer.PayOrderPaidListener.CONSUMER_RECHARGE;
import static com.ccloud.module.pay.mq.consumer.PayOrderPaidListener.toMillis;

/**
 * 退款结果：钱包充值的退款先处理充值（成功记退款成功，失败退回扣回的金额），再转发给绑定了开放应用的外部系统。
 * 两件事各自幂等。成功与失败各订阅一个主题，由子类提供主题与开放平台事件名
 */
@Slf4j
public abstract class AbstractPayRefundListener extends AbstractRedisStreamMessageListener<PayRefundMessage> {

    private final String event;

    @Resource
    private IdempotentConsumer idempotentConsumer;
    @Resource
    private PayWalletRechargeService rechargeService;
    @Resource
    private PayOpenNotifyService openNotifyService;

    protected AbstractPayRefundListener(String topic, String event) {
        super(topic, PayRefundMessage.class);
        this.event = event;
    }

    @Override
    public void onMessage(String messageId, PayRefundMessage message) {
        String status = statusName(message.getStatus());
        if (status == null) {
            // 不是明确的成功或失败：不动余额，也不向外部宣告一个无法断言的结果
            log.warn("[onMessage] 退款消息 {}（退款单 {}）状态 {} 不是成功或失败，忽略", messageId, message.getRefundNo(), message.getStatus());
            return;
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
            data.put("status", status);
            data.put("successTime", toMillis(message.getSuccessTime()));
            data.put("errorMsg", message.getChannelErrorMsg());
            idempotentConsumer.consumeOnce(messageId, CONSUMER_OPEN_NOTIFY,
                    () -> openNotifyService.forward(messageId, event, message.getAppId(), data));
        });
    }

    /**
     * @return SUCCESS 或 FAILURE；其他状态返回 null
     */
    private static String statusName(Integer status) {
        if (PayRefundStatusEnum.SUCCESS.getStatus().equals(status)) {
            return "SUCCESS";
        }
        if (PayRefundStatusEnum.FAILURE.getStatus().equals(status)) {
            return "FAILURE";
        }
        return null;
    }
}
