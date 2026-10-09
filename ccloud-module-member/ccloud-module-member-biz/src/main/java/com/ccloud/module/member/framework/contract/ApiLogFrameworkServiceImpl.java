package com.ccloud.module.member.framework.contract;

import cn.hutool.core.util.IdUtil;
import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.apilog.core.service.ApiAccessLogFrameworkService;
import com.ccloud.framework.apilog.core.service.ApiErrorLog;
import com.ccloud.framework.apilog.core.service.ApiErrorLogFrameworkService;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.mq.outbox.core.MessageSender;
import com.ccloud.module.infra.enums.logger.ApiLogTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * API 访问日志与异常日志经 Redis Stream 发给 infra 写库。
 *
 * <p>单线程有界队列异步发送：日志不能拖慢业务请求；Redis 故障或积压时丢弃并打警告，不影响业务
 */
@Slf4j
@Component
public class ApiLogFrameworkServiceImpl implements ApiAccessLogFrameworkService, ApiErrorLogFrameworkService,
        DisposableBean {

    private final MessageSender messageSender;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(10_000), r -> {
                Thread thread = new Thread(r, "api-log-sender");
                thread.setDaemon(true);
                return thread;
            }, (r, e) -> log.warn("[api-log] 发送队列已满，丢弃一条日志"));

    public ApiLogFrameworkServiceImpl(@Lazy MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public void createApiAccessLogAsync(ApiAccessLog accessLog) {
        send(ApiLogTopics.ACCESS_LOG, accessLog);
    }

    @Override
    public void createApiErrorLogAsync(ApiErrorLog errorLog) {
        send(ApiLogTopics.ERROR_LOG, errorLog);
    }

    private void send(String topic, Object payload) {
        executor.execute(() -> {
            try {
                messageSender.send(topic, IdUtil.fastSimpleUUID(), JsonUtils.toJsonString(payload));
            } catch (RuntimeException e) {
                log.warn("[api-log] 发送到 {} 失败，丢弃", topic, e);
            }
        });
    }

    @Override
    public void destroy() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
}
