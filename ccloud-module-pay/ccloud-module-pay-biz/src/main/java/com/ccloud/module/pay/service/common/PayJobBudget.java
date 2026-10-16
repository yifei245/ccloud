package com.ccloud.module.pay.service.common;

import java.time.Duration;

/**
 * 定时任务单次运行的收尾条件：时间预算用完，或所在线程被中断（调度中心超时、手动终止都会中断执行线程）。
 * 任务逐页处理积压时每处理一条就检查一次，及时退出，余下留给下一轮
 */
public final class PayJobBudget {

    private static final int MAX_CAUSE_DEPTH = 10;

    private final long deadlineNanos;

    private PayJobBudget(Duration budget) {
        this.deadlineNanos = System.nanoTime() + budget.toNanos();
    }

    public static PayJobBudget start(Duration budget) {
        return new PayJobBudget(budget);
    }

    /**
     * @return 线程已被中断，或时间预算已用完
     */
    public boolean exhausted() {
        return Thread.currentThread().isInterrupted() || System.nanoTime() - deadlineNanos >= 0;
    }

    /**
     * 逐条处理时捕获到的异常是否由线程中断引起（可能被包了几层）。
     * 是的话调用方须重新置位中断标记并停止本轮，不能当作普通失败继续处理下一条
     */
    public static boolean isInterruption(Throwable e) {
        // 限定深度：异常链可能成环（A 的 cause 是 B，B 的 cause 又是 A）
        Throwable t = e;
        for (int depth = 0; t != null && depth < MAX_CAUSE_DEPTH; depth++, t = t.getCause()) {
            if (t instanceof InterruptedException || t instanceof java.io.InterruptedIOException
                    || t instanceof java.nio.channels.ClosedByInterruptException) {
                return true;
            }
        }
        return false;
    }
}
