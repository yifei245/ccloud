package com.ccloud.module.pay.controller.admin.channel;

import com.ccloud.module.pay.support.FakePayClientProvider;
import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

// 此时还没有真实的渠道实现，用测试渠道（编码 mock，敏感字段 secret）验证脱敏
@Import(FakePayClientProvider.class)
class PayChannelControllerTest extends BasePayWebIntegrationTest {

    private static final String SECRET_VALUE = "0123456789abcdef-secret";

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM pay_channel WHERE app_id IN (SELECT id FROM (SELECT id FROM pay_app WHERE app_key LIKE 'webtest-%') t)");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key LIKE 'webtest-%'");
    }

    private long insertApp() {
        jdbcTemplate.update("INSERT INTO pay_app (app_key, name, status, tenant_id) VALUES ('webtest-app', 'Web测试应用', 0, ?)", TENANT);
        return jdbcTemplate.queryForObject("SELECT id FROM pay_app WHERE app_key = 'webtest-app' AND tenant_id = ?", Long.class, TENANT);
    }

    private static String body(long appId) {
        return "{\"appId\":" + appId + ",\"code\":\"mock\",\"status\":0,\"feeRate\":0.6,"
                + "\"config\":\"{\\\"secret\\\":\\\"" + SECRET_VALUE + "\\\"}\"}";
    }

    @Test
    void 查询渠道时密钥脱敏() throws Exception {
        long appId = insertApp();
        String createJson = mockMvc.perform(asAdmin(post("/admin-api/pay/channel/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body(appId)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        String id = createJson.replaceAll(".*\"data\":(\\d+).*", "$1");

        mockMvc.perform(asAdmin(get("/admin-api/pay/channel/get").param("id", id), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.config", not(containsString(SECRET_VALUE))))
                .andExpect(jsonPath("$.data.config", containsString("***")));
        mockMvc.perform(asAdmin(get("/admin-api/pay/channel/list").param("appId", String.valueOf(appId)), ADMIN_ID))
                .andExpect(jsonPath("$.data[0].config", not(containsString(SECRET_VALUE))));
    }

    @Test
    void 无权限返回403() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/pay/channel/create"), NO_PERM_ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body(1L)))
                .andExpect(jsonPath("$.code").value(403));
    }
}
