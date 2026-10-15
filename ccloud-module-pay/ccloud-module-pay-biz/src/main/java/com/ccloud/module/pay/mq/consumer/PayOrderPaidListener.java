package com.ccloud.module.pay.mq.consumer;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.ccloud.framework.mq.outbox.core.IdempotentConsumer;
import com.ccloud.framework.mq.redis.core.AbstractRedisStreamMessageListener;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.open.enums.notify.OpenNotifyEvents;
import com.ccloud.module.pay.mq.message.PayOrderMessage;
import com.ccloud.module.pay.service.notify.PayOpenNotifyService;
import com.ccloud.module.pay.service.wallet.PayWalletRechargeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付成功：钱包充值订单入账；绑定了开放应用的支付应用转发给外部系统。两件事各自幂等，
 * 其中一件失败时消息重投，已完成的那件会被跳过
 */
@Component
public class PayOrderPaidListener extends AbstractRedisStreamMessageListener<PayOrderMessage> {

    static final String CONSUMER_RECHARGE = "pay-wallet-recharge";
    static final String CONSUMER_OPEN_NOTIFY = "pay-open-notify";

    @Resource
    private IdempotentConsumer idempotentConsumer;
    @Resource
    private PayWalletRechargeService rechargeService;
    @Resource
    private PayOpenNotifyService openNotifyService;

    public PayOrderPaidListener() {
        super(PayOrderMessage.TOPIC_PAID, PayOrderMessage.class);
    }

    @Override
    public void onMessage(String messageId, PayOrderMessage message) {
        TenantUtils.execute(message.getTenantId(), () -> {
            if (rechargeService.isRechargeApp(message.getAppId())) {
                idempotentConsumer.consumeOnce(messageId, CONSUMER_RECHARGE, () -> rechargeService.handleRechargePaid(message));
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("merchantOrderId", message.getMerchantOrderId());
            data.put("orderNo", message.getOrderNo());
            data.put("price", message.getPrice());
            data.put("status", "SUCCESS");
            data.put("successTime", toMillis(message.getSuccessTime()));
            data.put("channelCode", message.getChannelCode());
            idempotentConsumer.consumeOnce(messageId, CONSUMER_OPEN_NOTIFY,
                    () -> openNotifyService.forward(messageId, OpenNotifyEvents.PAY_ORDER_PAID, message.getAppId(), data));
        });
    }

    /**
     * 回调数据中的时间一律为毫秒时间戳，没有则为 null
     */
    static Long toMillis(LocalDateTime time) {
        return time == null ? null : LocalDateTimeUtil.toEpochMilli(time);
    }
}
