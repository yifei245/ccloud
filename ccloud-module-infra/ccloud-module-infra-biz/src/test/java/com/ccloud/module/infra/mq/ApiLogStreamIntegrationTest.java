package com.ccloud.module.infra.mq;

import cn.hutool.core.util.IdUtil;
import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.apilog.core.service.ApiErrorLog;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.mq.outbox.core.MessageSender;
import com.ccloud.module.infra.enums.logger.ApiLogTopics;
import com.ccloud.module.infra.test.BaseInfraWebIntegrationTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 其余 Web 测试关闭了访问日志（避免向共享测试库留下日志），本类单独打开它，因此独占一个 Spring 上下文
 */
@TestPropertySource(properties = "ccloud.access-log.enable=true")
class ApiLogStreamIntegrationTest extends BaseInfraWebIntegrationTest {

    @Autowired
    private MessageSender messageSender;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM infra_api_access_log WHERE application_name IN ('ut-stream', 'ccloud-infra-server')");
        jdbcTemplate.update("DELETE FROM infra_api_error_log WHERE application_name = 'ut-stream'");
    }

    @Test
    void 其他服务发来的日志被消费写库() {
        ApiAccessLog accessLog = new ApiAccessLog();
        accessLog.setApplicationName("ut-stream");
        accessLog.setRequestUrl("/admin-api/ut/stream");
        accessLog.setTenantId(1L);
        accessLog.setBeginTime(LocalDateTime.now());
        accessLog.setEndTime(LocalDateTime.now());
        accessLog.setDuration(3);
        messageSender.send(ApiLogTopics.ACCESS_LOG, IdUtil.fastSimpleUUID(), JsonUtils.toJsonString(accessLog));

        ApiErrorLog errorLog = new ApiErrorLog();
        errorLog.setApplicationName("ut-stream");
        errorLog.setTenantId(1L);
        errorLog.setExceptionName("java.lang.IllegalStateException");
        messageSender.send(ApiLogTopics.ERROR_LOG, IdUtil.fastSimpleUUID(), JsonUtils.toJsonString(errorLog));

        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM infra_api_access_log WHERE application_name = 'ut-stream'", Long.class)).isEqualTo(1L);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM infra_api_error_log WHERE application_name = 'ut-stream'", Long.class)).isEqualTo(1L);
        });
    }

    @Test
    void infra自身的请求直接记录访问日志() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/infra/config/page"), ADMIN_ID));
        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM infra_api_access_log WHERE request_url = '/admin-api/infra/config/page' AND application_name = 'ccloud-infra-server'",
                Long.class) > 0);
    }
}
