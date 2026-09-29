package com.ccloud.framework.tenant.core.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextHolderTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("设置后应能读回租户编号")
    void setThenGet_shouldRoundTrip() {
        TenantContextHolder.setTenantId(7L);
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("clear 后应读不到租户编号")
    void clear_shouldResetContext() {
        TenantContextHolder.setTenantId(7L);
        TenantContextHolder.clear();
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    @Test
    @DisplayName("getRequiredTenantId 在无租户时应抛出可理解的异常")
    void getRequiredTenantId_shouldFailFastWhenAbsent() {
        assertThatThrownBy(TenantContextHolder::getRequiredTenantId)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("租户");
    }

    @Test
    @DisplayName("忽略标记应可独立设置与读取")
    void ignoreFlag_shouldBeIndependentlySettable() {
        TenantContextHolder.setIgnore(true);
        assertThat(TenantContextHolder.isIgnore()).isTrue();
        TenantContextHolder.setIgnore(false);
        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    @Test
    @DisplayName("借助 TransmittableThreadLocal，线程池中的任务应能继承租户上下文")
    void context_shouldPropagateIntoPooledThread() throws Exception {
        ExecutorService raw = Executors.newSingleThreadExecutor();
        // 先占用一次线程，确保后续提交复用的是已存在的线程——普通 InheritableThreadLocal 在此场景会失效
        raw.submit(() -> null).get();
        ExecutorService executor = com.alibaba.ttl.threadpool.TtlExecutors.getTtlExecutorService(raw);

        TenantContextHolder.setTenantId(42L);
        try {
            Future<Long> future = executor.submit(TenantContextHolder::getTenantId);
            assertThat(future.get(3, TimeUnit.SECONDS)).isEqualTo(42L);
        } finally {
            executor.shutdownNow();
        }
    }
}
