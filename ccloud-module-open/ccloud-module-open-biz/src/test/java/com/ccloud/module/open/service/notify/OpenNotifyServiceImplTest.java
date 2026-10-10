package com.ccloud.module.open.service.notify;

import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.tenant.core.service.SystemTenantValidator;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.open.dal.dataobject.app.OpenAppDO;
import com.ccloud.module.open.dal.dataobject.notify.OpenNotifyLogDO;
import com.ccloud.module.open.dal.mysql.app.OpenAppMapper;
import com.ccloud.module.open.enums.notify.OpenNotifyEvents;
import com.ccloud.module.open.enums.notify.OpenNotifyStatusEnum;
import com.ccloud.module.open.framework.config.OpenProperties;
import com.ccloud.module.open.framework.notify.OpenNotifyClient;
import com.ccloud.module.open.service.api.OpenApiServiceImpl;
import com.ccloud.module.open.service.app.OpenAppCacheInvalidator;
import com.ccloud.module.open.service.app.OpenAppServiceImpl;
import com.ccloud.module.open.support.NotifyReceiver;
import com.ccloud.module.open.test.BaseOpenDbUnitTest;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ccloud.module.open.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import({OpenNotifyServiceImpl.class, OpenNotifyClient.class, OpenAppServiceImpl.class, OpenApiServiceImpl.class})
@EnableConfigurationProperties(OpenProperties.class)
class OpenNotifyServiceImplTest extends BaseOpenDbUnitTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Resource
    private OpenNotifyService openNotifyService;
    @Resource
    private OpenAppMapper openAppMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private OpenAppCacheInvalidator invalidator;
    @MockitoBean
    private SystemTenantValidator systemTenantValidator;

    private NotifyReceiver receiver;
    private OpenNotifyServiceImpl impl;
    private OpenAppDO app;
    private Long appId;

    @BeforeEach
    void setUp() throws IOException {
        receiver = new NotifyReceiver();
        impl = (OpenNotifyServiceImpl) openNotifyService;
        appId = 新建应用(receiver.url("/n"));
    }

    @AfterEach
    void stopReceiver() {
        receiver.close();
    }

    @Test
    void 发送_写待通知记录_请求体含事件内容() {
        Long appId = 新建应用(receiver.url("/n"));
        Long id = openNotifyService.sendNotify(appId, "webtest.event", Map.of("k", "v"));
        OpenNotifyLogDO log = openNotifyService.getNotifyLog(id);
        assertThat(log.getStatus()).isEqualTo(OpenNotifyStatusEnum.WAITING.getStatus());
        assertThat(log.getEventId()).hasSize(32);
        Map<String, Object> body = JsonUtils.parseObject(log.getPayload(), new TypeReference<>() {});
        assertThat(body).containsEntry("event", "webtest.event").containsEntry("eventId", log.getEventId())
                .containsEntry("appKey", app.getAppKey()).containsKey("timestamp");
        assertThat(body.get("data")).isEqualTo(Map.of("k", "v"));
    }

    @Test
    void 应用没有回调地址_记为放弃() {
        Long noUrlAppId = 新建应用(null);
        Long id = openNotifyService.sendNotify(noUrlAppId, "webtest.event", Map.of());
        OpenNotifyLogDO log = openNotifyService.getNotifyLog(id);
        assertThat(log.getStatus()).isEqualTo(OpenNotifyStatusEnum.ABANDONED.getStatus());
        assertThat(log.getResponseBody()).isEqualTo("应用未配置回调地址");
        assertThat(log.getNextRetryTime()).isNull();
        assertThat(openNotifyService.executeDueNotifies(100)).isZero();
        assertThat(openNotifyService.getNotifyLog(id).getStatus()).isEqualTo(OpenNotifyStatusEnum.ABANDONED.getStatus());
        assertThat(receiver.requests).isEmpty();
    }

    @Test
    void 发送_应用不存在报错() {
        assertServiceException(() -> openNotifyService.sendNotify(-1L, "webtest.event", Map.of()), OPEN_APP_NOT_EXISTS);
    }

    @Test
    void 执行_成功_对方能验签() {
        Long id = openNotifyService.sendNotify(appId, "webtest.event", Map.of());
        assertThat(((OpenNotifyServiceImpl) openNotifyService).execute(id)).isTrue();
        OpenNotifyLogDO log = openNotifyService.getNotifyLog(id);
        assertThat(log.getStatus()).isEqualTo(OpenNotifyStatusEnum.SUCCESS.getStatus());
        assertThat(log.getHttpStatus()).isEqualTo(200);
        NotifyReceiver.Received r = receiver.requests.get(0);
        long ts = Long.parseLong(r.headers().getFirst("X-Open-Timestamp"));
        assertThat(r.headers().getFirst("X-Open-Sign"))
                .isEqualTo(OpenNotifySigner.sign(app.getAppSecret(), ts, log.getEventId(), r.body()));
        assertThat(r.headers().getFirst("X-Open-App-Id")).isEqualTo(app.getAppKey());
        assertThat(r.body()).isEqualTo(log.getPayload());
    }

    @Test
    void 执行_失败按退避_第6次重试仍失败放弃() {
        receiver.status = 500;
        Long id = openNotifyService.sendNotify(appId, "webtest.event", Map.of());
        LocalDateTime before = LocalDateTime.now();
        impl.execute(id);
        OpenNotifyLogDO log = openNotifyService.getNotifyLog(id);
        assertThat(log.getStatus()).isEqualTo(OpenNotifyStatusEnum.FAILURE.getStatus());
        assertThat(log.getRetryCount()).isEqualTo(1);
        assertThat(log.getHttpStatus()).isEqualTo(500);
        assertThat(log.getNextRetryTime()).isBetween(before.plusSeconds(14), LocalDateTime.now().plusSeconds(16));

        jdbcTemplate.update("UPDATE open_notify_log SET retry_count = 6, next_retry_time = NOW() - INTERVAL 1 SECOND WHERE id = ?", id);
        impl.execute(id);
        assertThat(openNotifyService.getNotifyLog(id).getStatus()).isEqualTo(OpenNotifyStatusEnum.ABANDONED.getStatus());
    }

    @Test
    void 退避间隔表() {
        assertThat(OpenNotifyServiceImpl.BACKOFF).containsExactly(Duration.ofSeconds(15), Duration.ofMinutes(1),
                Duration.ofMinutes(5), Duration.ofMinutes(30), Duration.ofHours(2), Duration.ofHours(6));
    }

    @Test
    void 已被抢占或未到期的记录不重复发送() {
        Long id = openNotifyService.sendNotify(appId, "webtest.event", Map.of());
        receiver.status = 500;
        impl.execute(id);                       // 失败，下次 15 秒后
        assertThat(impl.execute(id)).isFalse(); // 未到期
        assertThat(receiver.requests).hasSize(1);
    }

    @Test
    void 定时任务处理所有租户到期的记录() {
        Long tenant2AppId = TenantUtils.execute(2L, () -> 新建应用(receiver.url("/n")));
        Long id1 = openNotifyService.sendNotify(appId, "webtest.event", Map.of());
        Long id2 = openNotifyService.sendNotify(tenant2AppId, "webtest.event", Map.of());

        assertThat(openNotifyService.executeDueNotifies(100)).isEqualTo(2);

        assertThat(statusOf(id1)).isEqualTo(OpenNotifyStatusEnum.SUCCESS.getStatus());
        assertThat(statusOf(id2)).isEqualTo(OpenNotifyStatusEnum.SUCCESS.getStatus());
        assertThat(receiver.requests).hasSize(2);
    }

    @Test
    void 手工重新通知_只限失败与放弃_次数清零() {
        Long successId = openNotifyService.sendNotify(appId, "webtest.event", Map.of());
        impl.execute(successId);
        assertThat(statusOf(successId)).isEqualTo(OpenNotifyStatusEnum.SUCCESS.getStatus());
        assertServiceException(() -> openNotifyService.retryNotify(successId), OPEN_NOTIFY_LOG_CANNOT_RETRY);
        assertServiceException(() -> openNotifyService.retryNotify(-1L), OPEN_NOTIFY_LOG_NOT_EXISTS);

        Long abandonedId = openNotifyService.sendNotify(appId, "webtest.event", Map.of());
        jdbcTemplate.update("UPDATE open_notify_log SET status = 30, retry_count = 6 WHERE id = ?", abandonedId);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        openNotifyService.retryNotify(abandonedId);
        OpenNotifyLogDO log = openNotifyService.getNotifyLog(abandonedId);
        assertThat(log.getStatus()).isEqualTo(OpenNotifyStatusEnum.WAITING.getStatus());
        assertThat(log.getRetryCount()).isZero();
        assertThat(log.getNextRetryTime()).isBetween(before, LocalDateTime.now().plusSeconds(1));
    }

    @Test
    void 重试时使用应用当前的回调地址与密钥() throws IOException {
        receiver.status = 500;
        Long id = openNotifyService.sendNotify(appId, "webtest.event", Map.of());
        impl.execute(id);
        assertThat(receiver.requests).hasSize(1);

        try (NotifyReceiver other = new NotifyReceiver()) {
            openAppMapper.updateById(new OpenAppDO().setId(appId).setNotifyUrl(other.url("/n2"))
                    .setAppSecret("newsecret0123456789abcdefghijklm"));
            jdbcTemplate.update("UPDATE open_notify_log SET next_retry_time = NOW() - INTERVAL 1 SECOND WHERE id = ?", id);

            assertThat(impl.execute(id)).isTrue();

            assertThat(receiver.requests).hasSize(1);
            assertThat(other.requests).hasSize(1);
            NotifyReceiver.Received r = other.requests.get(0);
            long ts = Long.parseLong(r.headers().getFirst("X-Open-Timestamp"));
            assertThat(r.headers().getFirst("X-Open-Sign"))
                    .isEqualTo(OpenNotifySigner.sign("newsecret0123456789abcdefghijklm", ts, r.headers().getFirst("X-Open-Event-Id"), r.body()));
            OpenNotifyLogDO log = openNotifyService.getNotifyLog(id);
            assertThat(log.getStatus()).isEqualTo(OpenNotifyStatusEnum.SUCCESS.getStatus());
            assertThat(log.getNotifyUrl()).isEqualTo(other.url("/n2"));
        }
    }

    @Test
    void 测试回调_当前租户应用_没有回调地址报错() {
        Long id = openNotifyService.sendTestNotify(appId);
        OpenNotifyLogDO log = openNotifyService.getNotifyLog(id);
        assertThat(log.getEvent()).isEqualTo(OpenNotifyEvents.TEST);
        assertThat(log.getStatus()).isEqualTo(OpenNotifyStatusEnum.WAITING.getStatus());
        Map<String, Object> body = JsonUtils.parseObject(log.getPayload(), new TypeReference<>() {});
        assertThat(body).containsEntry("event", "open.test");
        assertThat(body.get("data")).isEqualTo(Map.of("message", "ccloud test"));

        Long noUrlAppId = 新建应用(null);
        assertServiceException(() -> openNotifyService.sendTestNotify(noUrlAppId), OPEN_APP_NOTIFY_URL_NOT_CONFIGURED);
        Long tenant2AppId = TenantUtils.execute(2L, () -> 新建应用(receiver.url("/n")));
        assertServiceException(() -> openNotifyService.sendTestNotify(tenant2AppId), OPEN_APP_NOT_EXISTS);
    }

    @Test
    void 清理只删除过期的成功与放弃记录() {
        Long waiting = openNotifyService.sendNotify(appId, "webtest.event", Map.of());
        Long success = openNotifyService.sendNotify(appId, "webtest.event", Map.of());
        Long failure = openNotifyService.sendNotify(appId, "webtest.event", Map.of());
        Long abandoned = openNotifyService.sendNotify(appId, "webtest.event", Map.of());
        Long recentSuccess = openNotifyService.sendNotify(appId, "webtest.event", Map.of());
        jdbcTemplate.update("UPDATE open_notify_log SET status = 10 WHERE id IN (?, ?)", success, recentSuccess);
        jdbcTemplate.update("UPDATE open_notify_log SET status = 20 WHERE id = ?", failure);
        jdbcTemplate.update("UPDATE open_notify_log SET status = 30 WHERE id = ?", abandoned);
        jdbcTemplate.update("UPDATE open_notify_log SET create_time = ? WHERE id IN (?, ?, ?, ?)",
                LocalDateTime.now().minusDays(40), waiting, success, failure, abandoned);

        assertThat(openNotifyService.cleanNotifyLog(30, 100)).isEqualTo(2);

        assertThat(jdbcTemplate.queryForList("SELECT id FROM open_notify_log WHERE app_id = ? ORDER BY id", Long.class, appId))
                .containsExactly(waiting, failure, recentSuccess);
    }

    private Integer statusOf(Long id) {
        return TenantUtils.executeIgnore(() -> openNotifyService.getNotifyLog(id)).getStatus();
    }

    /**
     * 在当前租户下新建应用，记录到 {@link #app}
     */
    private Long 新建应用(String notifyUrl) {
        int seq = SEQ.incrementAndGet();
        OpenAppDO created = new OpenAppDO().setName("Web测试").setAppKey(String.format("webtestnotify%03d", seq))
                .setAppSecret("secret0123456789abcdefghijklmnop").setStatus(0).setRateLimit(100).setNotifyUrl(notifyUrl);
        openAppMapper.insert(created);
        app = created;
        return created.getId();
    }
}
