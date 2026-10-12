package com.ccloud.module.open.mq.consumer.logger;

import cn.hutool.core.util.IdUtil;
import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.open.dal.dataobject.app.OpenAppDO;
import com.ccloud.module.open.dal.mysql.app.OpenAppMapper;
import com.ccloud.module.open.enums.logger.OpenApiLogTopics;
import com.ccloud.module.open.test.BaseOpenWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 真实 Redis Stream：向 open.api-log 流追加消息，由应用中的监听器消费入库
 */
class OpenApiLogMessageListenerTest extends BaseOpenWebIntegrationTest {

    private static final String STREAM_KEY = "ccloud:stream:" + OpenApiLogTopics.API_LOG;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private OpenAppMapper openAppMapper;

    private final List<RecordId> recordIds = new ArrayList<>();
    private OpenAppDO app;

    @BeforeEach
    void insertApp() {
        app = new OpenAppDO().setName("Web测试日志").setAppKey("webtestkey000002").setAppSecret("secret0123456789")
                .setStatus(0).setRateLimit(100);
        TenantUtils.execute(2L, () -> openAppMapper.insert(app));
    }

    @AfterEach
    void cleanUp() {
        // 只删本测试追加的消息，不动流中的其他消息
        for (RecordId recordId : recordIds) {
            stringRedisTemplate.opsForStream().delete(STREAM_KEY, recordId);
        }
        jdbcTemplate.update("DELETE FROM open_api_log WHERE trace_id LIKE 'webtest-mq-%'");
        jdbcTemplate.update("DELETE FROM open_app WHERE name LIKE 'Web测试日志%'");
    }

    @Test
    void 流中的日志消息被消费入库_租户与应用取自消息() {
        ApiAccessLog log = new ApiAccessLog();
        log.setUserId(app.getId());
        log.setUserType(3);
        log.setTenantId(2L);
        log.setRequestMethod("GET");
        log.setRequestUrl("/open-api/webtest/mq?x=1");
        log.setResultCode(0);
        log.setUserIp("10.0.0.2");
        log.setTraceId("webtest-mq-1");
        log.setBeginTime(LocalDateTime.now());
        log.setEndTime(LocalDateTime.now());
        log.setDuration(3);

        send(log);

        await().atMost(Duration.ofSeconds(10)).until(() -> jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM open_api_log WHERE trace_id = 'webtest-mq-1'", Integer.class) == 1);
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT tenant_id, app_id, app_key, request_url, user_ip FROM open_api_log WHERE trace_id = 'webtest-mq-1'");
        assertThat(((Number) row.get("tenant_id")).longValue()).isEqualTo(2L);
        assertThat(((Number) row.get("app_id")).longValue()).isEqualTo(app.getId());
        assertThat(row.get("app_key")).isEqualTo("webtestkey000002");
        assertThat(row.get("request_url")).isEqualTo("/open-api/webtest/mq?x=1");
        assertThat(row.get("user_ip")).isEqualTo("10.0.0.2");
    }

    private void send(ApiAccessLog log) {
        MapRecord<String, String, String> record = StreamRecords.newRecord().in(STREAM_KEY).ofMap(Map.of(
                "messageId", IdUtil.fastSimpleUUID(), "payload", JsonUtils.toJsonString(log)));
        recordIds.add(stringRedisTemplate.opsForStream().add(record));
    }
}
