package com.ccloud.module.pay.controller.admin.channel;

import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.apilog.core.service.ApiErrorLog;
import com.ccloud.module.pay.controller.admin.channel.vo.PayChannelSaveReqVO;
import com.ccloud.module.pay.framework.contract.ApiLogFrameworkServiceImpl;
import com.ccloud.module.pay.service.channel.PayChannelService;
import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 渠道配置是 JSON 字符串，里面的密钥名脱敏器看不到：靠 @ApiAccessLog(sanitizeKeys = "config") 整段屏蔽，
 * 访问日志与异常日志（共用同一套规则）都不能带出配置原文
 */
@TestPropertySource(properties = "ccloud.access-log.enable=true")
class PayChannelSecretLogTest extends BasePayWebIntegrationTest {

    private static final String SECRET_VALUE = "log-leak-canary-0123456789";

    @MockitoSpyBean
    private ApiLogFrameworkServiceImpl apiLogService;
    @MockitoSpyBean
    private PayChannelService payChannelService;

    @BeforeEach
    void silenceSender() {
        // 只截取日志对象，不真的发到 Redis 流
        doNothing().when(apiLogService).createApiAccessLogAsync(any());
        doNothing().when(apiLogService).createApiErrorLogAsync(any());
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM pay_channel WHERE app_id IN (SELECT id FROM (SELECT id FROM pay_app WHERE app_key LIKE 'webtest-%') t)");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key LIKE 'webtest-%'");
    }

    private static String body(String idPart, long appId) {
        return "{" + idPart + "\"appId\":" + appId + ",\"code\":\"mock\",\"status\":0,\"feeRate\":0.6,"
                + "\"config\":\"{\\\"notifySecret\\\":\\\"" + SECRET_VALUE + "\\\",\\\"clientKey\\\":\\\"" + SECRET_VALUE
                + "\\\",\\\"behavior\\\":\\\"SUCCESS\\\"}\"}";
    }

    @Test
    @DisplayName("创建与修改渠道的访问日志不含渠道配置原文")
    void accessLogMasksChannelConfig() throws Exception {
        jdbcTemplate.update("INSERT INTO pay_app (app_key, name, status, tenant_id) VALUES ('webtest-log-app', '日志脱敏', 0, ?)", TENANT);
        long appId = jdbcTemplate.queryForObject("SELECT id FROM pay_app WHERE app_key = 'webtest-log-app'", Long.class);

        String created = mockMvc.perform(asAdmin(post("/admin-api/pay/channel/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body("", appId)))
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(created.replaceAll(".*\"data\":(\\d+).*", "$1"));
        mockMvc.perform(asAdmin(put("/admin-api/pay/channel/update"), ADMIN_ID)
                .contentType(MediaType.APPLICATION_JSON).content(body("\"id\":" + id + ",", appId)));

        ArgumentCaptor<ApiAccessLog> captor = ArgumentCaptor.forClass(ApiAccessLog.class);
        verify(apiLogService, timeout(5000).atLeast(2)).createApiAccessLogAsync(captor.capture());
        var logs = captor.getAllValues().stream()
                .filter(l -> l.getRequestUrl().startsWith("/admin-api/pay/channel/")).toList();
        assertThat(logs).hasSize(2);
        for (ApiAccessLog l : logs) {
            assertThat(l.getRequestParams()).isNotNull()
                    .doesNotContain(SECRET_VALUE).doesNotContain("notifySecret")
                    .contains("\"config\":\"******\"")
                    .contains("\"appId\":" + appId);
        }
    }

    @Test
    @DisplayName("创建渠道时发生未预期异常，异常日志的请求参数也不含渠道配置原文")
    void errorLogMasksChannelConfig() throws Exception {
        doThrow(new IllegalStateException("boom")).when(payChannelService).createChannel(any(PayChannelSaveReqVO.class));

        mockMvc.perform(asAdmin(post("/admin-api/pay/channel/create"), ADMIN_ID)
                .contentType(MediaType.APPLICATION_JSON).content(body("", 1L)));

        ArgumentCaptor<ApiErrorLog> captor = ArgumentCaptor.forClass(ApiErrorLog.class);
        verify(apiLogService, timeout(5000)).createApiErrorLogAsync(captor.capture());
        assertThat(captor.getValue().getRequestParams()).isNotNull()
                .doesNotContain(SECRET_VALUE).contains("\"config\":\"******\"");
    }
}
