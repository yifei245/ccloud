package com.ccloud.module.pay.mq.consumer;

import cn.hutool.core.date.LocalDateTimeUtil;

import java.time.LocalDateTime;

/**
 * 消息监听器共用的常量与工具
 */
final class PayConsumers {

    /** 钱包充值入账（支付成功、充值退款结果） */
    static final String CONSUMER_RECHARGE = "pay-wallet-recharge";
    /** 经开放平台向外部系统回调 */
    static final String CONSUMER_OPEN_NOTIFY = "pay-open-notify";

    private PayConsumers() {
    }

    /**
     * 回调数据中的时间一律为毫秒时间戳，没有则为 null
     */
    static Long toMillis(LocalDateTime time) {
        return time == null ? null : LocalDateTimeUtil.toEpochMilli(time);
    }

    /**
     * 消息必须带租户：没有租户就不能进入租户上下文，业务代码会在无租户范围下运行。
     * 抛异常让消息重投，最终进入死信，而不是被静默确认
     */
    static void requireTenant(String messageId, String topic, Long tenantId) {
        if (tenantId == null) {
            throw new IllegalStateException("消息 " + messageId + "（主题 " + topic + "）没有租户编号，拒绝处理");
        }
    }
}
