package com.ccloud.module.pay.service.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class PayJobBudgetTest {

    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    @DisplayName("预算未用完且线程未中断时可继续；预算用完后结束")
    void exhaustedByTime() {
        assertThat(PayJobBudget.start(Duration.ofSeconds(30)).exhausted()).isFalse();
        assertThat(PayJobBudget.start(Duration.ZERO).exhausted()).isTrue();
        assertThat(PayJobBudget.start(Duration.ofSeconds(-1)).exhausted()).isTrue();
    }

    @Test
    @DisplayName("线程被中断时立即结束，且不清除中断标记")
    void exhaustedByInterrupt() {
        PayJobBudget budget = PayJobBudget.start(Duration.ofSeconds(30));
        Thread.currentThread().interrupt();
        assertThat(budget.exhausted()).isTrue();
        assertThat(Thread.currentThread().isInterrupted()).as("检查不得吞掉中断标记").isTrue();
    }

    @Test
    @DisplayName("异常链成环时有限步内结束，不死循环；链很深时也只看前若干层")
    void isInterruptionBoundedOnCycle() {
        RuntimeException a = new RuntimeException("a");
        RuntimeException b = new RuntimeException("b");
        a.initCause(b);
        b.initCause(a);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThat(PayJobBudget.isInterruption(a)).isFalse());

        // 环上挂着中断：能在深度上限内找到
        RuntimeException c = new RuntimeException("c", new InterruptedException());
        assertThat(PayJobBudget.isInterruption(new RuntimeException(new RuntimeException(c)))).isTrue();

        Throwable deep = new InterruptedException();
        for (int i = 0; i < 30; i++) {
            deep = new RuntimeException(deep);
        }
        assertThat(PayJobBudget.isInterruption(deep)).as("超过深度上限不再追").isFalse();
    }

    @Test
    @DisplayName("异常链中含 InterruptedException 才算中断")
    void isInterruption() {
        assertThat(PayJobBudget.isInterruption(new InterruptedException())).isTrue();
        assertThat(PayJobBudget.isInterruption(new RuntimeException("wrap", new IllegalStateException(new InterruptedException())))).isTrue();
        assertThat(PayJobBudget.isInterruption(new IllegalStateException("timeout"))).isFalse();
        assertThat(PayJobBudget.isInterruption(null)).isFalse();
    }
}
