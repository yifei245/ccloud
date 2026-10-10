package com.ccloud.module.open.controller.admin.notify;

import com.ccloud.module.open.test.BaseOpenWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class OpenNotifyLogControllerTest extends BaseOpenWebIntegrationTest {

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM open_notify_log WHERE event LIKE 'webtest.%'");
    }

    @Test
    void 分页按租户隔离() throws Exception {
        insertLog("webtest-ctl-1", 1L, 10);
        insertLog("webtest-ctl-2", 2L, 10);
        insertLog("webtest-ctl-3", 1L, 20);

        mockMvc.perform(asAdmin(get("/admin-api/open/notify-log/page").param("event", "webtest.ctl"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list", hasSize(2)))
                .andExpect(jsonPath("$.data.list[0].eventId").value("webtest-ctl-3"))
                .andExpect(jsonPath("$.data.list[1].eventId").value("webtest-ctl-1"))
                .andExpect(jsonPath("$.data.list[0].tenantId").value(1));
        mockMvc.perform(asAdmin(get("/admin-api/open/notify-log/page").param("event", "webtest.ctl")
                        .param("status", "20"), ADMIN_ID))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].eventId").value("webtest-ctl-3"));
        mockMvc.perform(asAdminOfTenant(get("/admin-api/open/notify-log/page").param("event", "webtest.ctl"), ADMIN_ID, TENANT_2))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].eventId").value("webtest-ctl-2"));
    }

    @Test
    void 详情按租户隔离() throws Exception {
        long id = insertLog("webtest-ctl-get", 1L, 10);
        mockMvc.perform(asAdmin(get("/admin-api/open/notify-log/get").param("id", String.valueOf(id)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.eventId").value("webtest-ctl-get"))
                .andExpect(jsonPath("$.data.payload").value("{}"))
                .andExpect(jsonPath("$.data.status").value(10));
        mockMvc.perform(asAdminOfTenant(get("/admin-api/open/notify-log/get").param("id", String.valueOf(id)), ADMIN_ID, TENANT_2))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 成功的记录不能重新通知() throws Exception {
        long id = insertLog("webtest-ctl-retry", 1L, 10);
        mockMvc.perform(asAdmin(put("/admin-api/open/notify-log/retry").param("id", String.valueOf(id)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(1_005_003_001));
    }

    @Test
    void 无权限管理员返回403() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/open/notify-log/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/open/notify-log/get").param("id", "1"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(put("/admin-api/open/notify-log/retry").param("id", "1"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }

    private long insertLog(String eventId, long tenantId, int status) {
        jdbcTemplate.update("INSERT INTO open_notify_log (app_id, event_id, event, payload, notify_url, status, retry_count, tenant_id) "
                + "VALUES (?, ?, 'webtest.ctl', '{}', 'https://example.com/notify', ?, 0, ?)", 1L, eventId, status, tenantId);
        return jdbcTemplate.queryForObject("SELECT id FROM open_notify_log WHERE event_id = ?", Long.class, eventId);
    }
}
