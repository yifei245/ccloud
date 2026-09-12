package com.ccloud.framework.mq.outbox;

import com.ccloud.framework.mq.outbox.core.MessageSender;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 可控的投递器，用于在无 Redis 的环境下验证投递时机与失败处理
 */
public class StubMessageSender implements MessageSender {

    private final AtomicInteger sentCount = new AtomicInteger();
    private volatile boolean failing = false;
    private volatile Runnable onSend;

    @Override
    public void send(String topic, String messageId, String payload) {
        if (onSend != null) {
            onSend.run();
        }
        if (failing) {
            throw new IllegalStateException("模拟投递失败");
        }
        sentCount.incrementAndGet();
    }

    public int sentCount() {
        return sentCount.get();
    }

    public void setFailing(boolean failing) {
        this.failing = failing;
    }

    public void setOnSend(Runnable onSend) {
        this.onSend = onSend;
    }

    public void reset() {
        sentCount.set(0);
        failing = false;
        onSend = null;
    }
}
