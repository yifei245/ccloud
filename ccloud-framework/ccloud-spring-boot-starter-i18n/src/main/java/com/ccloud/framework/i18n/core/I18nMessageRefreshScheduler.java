package com.ccloud.framework.i18n.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 服务就绪后立即加载一次文案，此后定时兜底刷新：覆盖刷新通知丢失、启动时 system 尚不可用的情况
 */
@Slf4j
public class I18nMessageRefreshScheduler implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private final I18nMessageRefresher refresher;
    private final Duration interval;
    private ScheduledExecutorService executor;

    public I18nMessageRefreshScheduler(I18nMessageRefresher refresher, Duration interval) {
        this.refresher = refresher;
        this.interval = interval;
    }

    @Override
    public synchronized void onApplicationEvent(ApplicationReadyEvent event) {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "i18n-refresh");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::safeRefresh, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void safeRefresh() {
        try {
            refresher.refresh();
        } catch (Throwable e) {
            // 定时任务抛出异常会被取消，后续再也不会刷新
            log.warn("[safeRefresh] 刷新文案异常", e);
        }
    }

    @Override
    public synchronized void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
