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

import static com.ccloud.module.pay.mq.consumer.PayOrderPaidListener.CONSUMER_OPEN_NOTIFY;
import static com.ccloud.module.pay.mq.consumer.PayOrderPaidListener.toMillis;

/**
 * 转账结果：转发给绑定了开放应用的外部系统。成功与失败各订阅一个主题，由子类提供主题与开放平台事件名
 */
@Slf4j
public abstract class AbstractPayTransferListener extends AbstractRedisStreamMessageListener<PayTransferMessage> {

    private final String event;

    @Resource
    private IdempotentConsumer idempotentConsumer;
    @Resource
    private PayOpenNotifyService openNotifyService;

    protected AbstractPayTransferListener(String topic, String event) {
        super(topic, PayTransferMessage.class);
        this.event = event;
    }

    @Override
    public void onMessage(String messageId, PayTransferMessage message) {
        String status = statusName(message.getStatus());
        if (status == null) {
            // 不是明确的成功或失败：不向外部宣告一个无法断言的结果
            log.warn("[onMessage] 转账消息 {}（转账单 {}）状态 {} 不是成功或失败，忽略", messageId, message.getTransferNo(), message.getStatus());
            return;
        }
        TenantUtils.execute(message.getTenantId(), () -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("merchantTransferId", message.getMerchantTransferId());
            data.put("transferNo", message.getTransferNo());
            data.put("price", message.getPrice());
            data.put("status", status);
            data.put("successTime", toMillis(message.getSuccessTime()));
            data.put("errorMsg", message.getChannelErrorMsg());
            idempotentConsumer.consumeOnce(messageId, CONSUMER_OPEN_NOTIFY,
                    () -> openNotifyService.forward(messageId, event, message.getAppId(), data));
        });
    }

    /**
     * @return SUCCESS 或 FAILURE；其他状态（等待、进行中、空、未知）返回 null
     */
    private static String statusName(Integer status) {
        if (PayTransferStatusEnum.SUCCESS.getStatus().equals(status)) {
            return "SUCCESS";
        }
        if (PayTransferStatusEnum.FAILURE.getStatus().equals(status)) {
            return "FAILURE";
        }
        return null;
    }
}
