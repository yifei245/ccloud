package com.ccloud.module.infra.framework.contract;

import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.apilog.core.service.ApiAccessLogFrameworkService;
import com.ccloud.framework.apilog.core.service.ApiErrorLog;
import com.ccloud.framework.apilog.core.service.ApiErrorLogFrameworkService;
import com.ccloud.module.infra.service.logger.ApiAccessLogService;
import com.ccloud.module.infra.service.logger.ApiErrorLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * infra 自身的 API 访问日志与异常日志直接写库，不绕道 Redis Stream。
 *
 * <p>单线程有界队列异步写入：日志不能拖慢业务请求；数据库故障或积压时丢弃并打警告，不影响业务
 */
@Slf4j
@Component
public class ApiLogFrameworkServiceImpl implements ApiAccessLogFrameworkService, ApiErrorLogFrameworkService,
        DisposableBean {

    @Lazy
    @Resource
    private ApiAccessLogService apiAccessLogService;
    @Lazy
    @Resource
    private ApiErrorLogService apiErrorLogService;

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(10_000), r -> {
                Thread thread = new Thread(r, "api-log-writer");
                thread.setDaemon(true);
                return thread;
            }, (r, e) -> log.warn("[api-log] 写入队列已满，丢弃一条日志"));

    @Override
    public void createApiAccessLogAsync(ApiAccessLog accessLog) {
        write("访问日志", () -> apiAccessLogService.createApiAccessLog(accessLog));
    }

    @Override
    public void createApiErrorLogAsync(ApiErrorLog errorLog) {
        write("异常日志", () -> apiErrorLogService.createApiErrorLog(errorLog));
    }

    private void write(String type, Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                log.warn("[api-log] 写入{}失败，丢弃", type, e);
            }
        });
    }

    @Override
    public void destroy() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
}
