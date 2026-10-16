package com.ccloud.module.pay.service.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("异常链中含 InterruptedException 才算中断")
    void isInterruption() {
        assertThat(PayJobBudget.isInterruption(new InterruptedException())).isTrue();
        assertThat(PayJobBudget.isInterruption(new RuntimeException("wrap", new IllegalStateException(new InterruptedException())))).isTrue();
        assertThat(PayJobBudget.isInterruption(new IllegalStateException("timeout"))).isFalse();
        assertThat(PayJobBudget.isInterruption(null)).isFalse();
    }
}
