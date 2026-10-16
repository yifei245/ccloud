package com.ccloud.module.open.service.notify;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.open.controller.admin.notify.vo.OpenNotifyLogPageReqVO;
import com.ccloud.module.open.dal.dataobject.app.OpenAppDO;
import com.ccloud.module.open.dal.dataobject.notify.OpenNotifyLogDO;
import com.ccloud.module.open.dal.mysql.notify.OpenNotifyLogMapper;
import com.ccloud.module.open.enums.notify.OpenNotifyEvents;
import com.ccloud.module.open.enums.notify.OpenNotifyStatusEnum;
import com.ccloud.module.open.framework.notify.NotifyResult;
import com.ccloud.module.open.framework.notify.OpenNotifyClient;
import com.ccloud.module.open.service.app.OpenAppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.open.enums.ErrorCodeConstants.*;

@Slf4j
@Service
public class OpenNotifyServiceImpl implements OpenNotifyService, DisposableBean {

    static final Duration[] BACKOFF = {Duration.ofSeconds(15), Duration.ofMinutes(1), Duration.ofMinutes(5),
            Duration.ofMinutes(30), Duration.ofHours(2), Duration.ofHours(6)};
    /**
     * 抢占后的租约：发送最长约 10 秒（连接 + 读取超时），租约内其他执行方不会重复发送；进程在发送中崩溃时租约到期后由定时任务接手
     */
    static final Duration LEASE = Duration.ofSeconds(60);

    @Resource
    private OpenNotifyLogMapper openNotifyLogMapper;
    @Resource
    private OpenAppService openAppService;
    @Resource
    private OpenNotifyClient openNotifyClient;

    /**
     * 即时发送用的线程池。队列满时丢弃：记录已落库，定时任务会接手
     */
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1000), r -> {
                Thread thread = new Thread(r, "open-notify");
                thread.setDaemon(true);
                return thread;
            }, (r, e) -> log.warn("[open-notify] 即时发送队列已满，交由定时任务发送"));

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendNotify(Long appId, String eventId, String event, Map<String, Object> data) {
        if (StrUtil.isNotBlank(eventId)) {
            // 调用方重投同一事件（如消费事务提交失败后消息重投）时不重复回调
            OpenNotifyLogDO existing = TenantUtils.executeIgnore(() -> openNotifyLogMapper.selectByEventId(eventId));
            if (existing != null) {
                return existing.getId();
            }
        }
        OpenAppDO app = openAppService.getAppIgnoreTenant(appId);
        if (app == null) {
            throw exception(OPEN_APP_NOT_EXISTS);
        }
        String finalEventId = StrUtil.isNotBlank(eventId) ? eventId : IdUtil.fastSimpleUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", finalEventId);
        body.put("event", event);
        body.put("appKey", app.getAppKey());
        body.put("timestamp", System.currentTimeMillis());
        body.put("data", data);
        boolean noUrl = StrUtil.isBlank(app.getNotifyUrl());
        OpenNotifyLogDO notifyLog = new OpenNotifyLogDO().setAppId(appId).setEventId(finalEventId).setEvent(event)
                .setPayload(JsonUtils.toJsonString(body))
                .setNotifyUrl(StrUtil.nullToEmpty(app.getNotifyUrl()))
                .setStatus(noUrl ? OpenNotifyStatusEnum.ABANDONED.getStatus() : OpenNotifyStatusEnum.WAITING.getStatus())
                .setRetryCount(0)
                .setNextRetryTime(noUrl ? null : dueNow())
                .setResponseBody(noUrl ? "应用未配置回调地址" : null);
        // 回调记录属于应用所在租户；调用方可能是其他租户上下文中的业务服务
        notifyLog.setTenantId(app.getTenantId());
        try {
            TenantUtils.executeIgnore(() -> openNotifyLogMapper.insert(notifyLog));
        } catch (DuplicateKeyException e) {
            // 并发重投：另一线程已写入同一事件。可重复读下前面的查询已固定快照，须加锁读取才能看到对方已提交的记录
            return TenantUtils.executeIgnore(() -> openNotifyLogMapper.selectIdByEventIdForShare(finalEventId));
        }
        if (!noUrl) {
            executeAfterCommit(notifyLog.getId());
        }
        return notifyLog.getId();
    }

    @Override
    public Long sendTestNotify(Long appId) {
        OpenAppDO app = openAppService.getApp(appId);
        if (app == null) {
            throw exception(OPEN_APP_NOT_EXISTS);
        }
        if (StrUtil.isBlank(app.getNotifyUrl())) {
            throw exception(OPEN_APP_NOTIFY_URL_NOT_CONFIGURED);
        }
        return sendNotify(appId, null, OpenNotifyEvents.TEST, Map.of("message", "ccloud test"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryNotify(Long id) {
        if (getNotifyLog(id) == null) {
            throw exception(OPEN_NOTIFY_LOG_NOT_EXISTS);
        }
        // 条件更新而非先查后改：查询之后定时任务可能已把记录写成成功，无条件更新会把它重置并重发
        int updated = openNotifyLogMapper.updateByIdAndStatus(id,
                List.of(OpenNotifyStatusEnum.FAILURE.getStatus(), OpenNotifyStatusEnum.ABANDONED.getStatus()),
                new OpenNotifyLogDO().setStatus(OpenNotifyStatusEnum.WAITING.getStatus()).setRetryCount(0)
                        .setNextRetryTime(dueNow()));
        if (updated == 0) {
            throw exception(OPEN_NOTIFY_LOG_CANNOT_RETRY);
        }
        executeAfterCommit(id);
    }

    @Override
    public int executeDueNotifies(int limit) {
        List<Long> ids = TenantUtils.executeIgnore(() -> openNotifyLogMapper.selectDueIds(LocalDateTime.now(), limit));
        int count = 0;
        for (Long id : ids) {
            try {
                if (execute(id)) {
                    count++;
                }
            } catch (RuntimeException e) {
                // 单条异常不影响其他记录；租约到期后会再次被扫到
                log.error("[executeDueNotifies] 回调 {} 处理异常", id, e);
            }
        }
        return count;
    }

    /**
     * 抢占并发送一次，按结果更新状态。返回是否抢占成功
     */
    boolean execute(Long id) {
        // 先读后抢占：抢占以读到的重试次数为条件，计算退避用的次数与抢占时一致
        OpenNotifyLogDO notifyLog = TenantUtils.executeIgnore(() -> openNotifyLogMapper.selectById(id));
        if (notifyLog == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        // 租约结束时间截到整秒，与库中存储值一致，回写时据此确认仍持有租约
        LocalDateTime lease = now.plus(LEASE).truncatedTo(ChronoUnit.SECONDS);
        if (TenantUtils.executeIgnore(() -> openNotifyLogMapper.claim(id, notifyLog.getRetryCount(), now, lease)) == 0) {
            return false;
        }
        OpenAppDO app = openAppService.getAppIgnoreTenant(notifyLog.getAppId());
        OpenNotifyLogDO update = new OpenNotifyLogDO();
        NotifyResult result;
        if (app == null || StrUtil.isBlank(app.getNotifyUrl())) {
            result = new NotifyResult(false, 0, app == null ? "应用已删除" : "应用未配置回调地址");
            update.setStatus(OpenNotifyStatusEnum.ABANDONED.getStatus());
        } else {
            // 重试时用应用当前的地址与密钥：管理员修正回调地址、重置密钥后，后续重试随之生效
            long timestamp = System.currentTimeMillis();
            Map<String, String> headers = Map.of(
                    "X-Open-App-Id", app.getAppKey(),
                    "X-Open-Timestamp", String.valueOf(timestamp),
                    "X-Open-Event-Id", notifyLog.getEventId(),
                    "X-Open-Sign", OpenNotifySigner.sign(app.getAppSecret(), timestamp, notifyLog.getEventId(),
                            notifyLog.getPayload()));
            update.setNotifyUrl(app.getNotifyUrl());
            result = openNotifyClient.post(app.getNotifyUrl(), headers, notifyLog.getPayload());
            if (result.success()) {
                update.setStatus(OpenNotifyStatusEnum.SUCCESS.getStatus());
            } else {
                int retried = notifyLog.getRetryCount();
                if (retried < BACKOFF.length) {
                    update.setStatus(OpenNotifyStatusEnum.FAILURE.getStatus()).setRetryCount(retried + 1)
                            .setNextRetryTime(LocalDateTime.now().plus(BACKOFF[retried]));
                } else {
                    update.setStatus(OpenNotifyStatusEnum.ABANDONED.getStatus());
                }
            }
        }
        update.setHttpStatus(result.httpStatus()).setResponseBody(result.responseBody());
        if (TenantUtils.executeIgnore(() -> openNotifyLogMapper.updateIfLeaseHeld(update, id, lease,
                notifyLog.getRetryCount())) == 0) {
            // 租约已失效（超时后被他方重新抢占、发送中被手工重新通知等），结果以持有租约的一方为准
            log.warn("[execute] 回调 {} 的租约已失效，丢弃本次发送结果：{}", id, result);
        }
        return true;
    }

    /**
     * 立即到期的下次重试时间。截到整秒：datetime 列按四舍五入保存毫秒，不截断时可能存成下一秒，
     * 紧接着的抢占（next_retry_time <= now）会落空
     */
    private static LocalDateTime dueNow() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private void executeAfterCommit(Long id) {
        Runnable task = () -> executor.execute(() -> {
            try {
                execute(id);
            } catch (RuntimeException e) {
                log.error("[executeAfterCommit] 回调 {} 即时发送异常，交由定时任务重试", id, e);
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    @Override
    public OpenNotifyLogDO getNotifyLog(Long id) {
        return openNotifyLogMapper.selectById(id);
    }

    @Override
    public PageResult<OpenNotifyLogDO> getNotifyLogPage(OpenNotifyLogPageReqVO reqVO) {
        return openNotifyLogMapper.selectPage(reqVO);
    }

    @Override
    public int cleanNotifyLog(int retainDays, int batchSize) {
        LocalDateTime before = LocalDateTime.now().minusDays(retainDays);
        int total = 0;
        int deleted;
        do {
            deleted = TenantUtils.executeIgnore(() -> openNotifyLogMapper.deleteFinishedByCreateTimeLt(before, batchSize));
            total += deleted;
        } while (deleted >= batchSize);
        return total;
    }

    @Override
    public void destroy() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
}
