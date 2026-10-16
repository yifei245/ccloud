package com.ccloud.module.pay.support;

import com.ccloud.framework.mq.outbox.core.IdempotentConsumer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * IdempotentConsumer 的内存桩，语义与真实实现一致：同一 (消息编号, 消费者) 只执行一次；
 * 动作抛异常时不记为已消费（真实实现里是日志随事务回滚），下次投递会再执行
 */
public final class IdempotentConsumerStub {

    /** 每次调用 consumeOnce 的消费者名，含被跳过的 */
    public final List<String> calls = new ArrayList<>();
    /** 已成功消费的 "消息编号|消费者名" */
    public final Set<String> consumed = new HashSet<>();

    private IdempotentConsumerStub() {
    }

    public static IdempotentConsumerStub install(IdempotentConsumer mock) {
        IdempotentConsumerStub stub = new IdempotentConsumerStub();
        lenient().when(mock.consumeOnce(anyString(), anyString(), any())).thenAnswer(inv -> {
            String messageId = inv.getArgument(0);
            String consumer = inv.getArgument(1);
            stub.calls.add(consumer);
            String key = messageId + "|" + consumer;
            if (stub.consumed.contains(key)) {
                return false;
            }
            inv.getArgument(2, Runnable.class).run();
            stub.consumed.add(key);
            return true;
        });
        return stub;
    }
}
