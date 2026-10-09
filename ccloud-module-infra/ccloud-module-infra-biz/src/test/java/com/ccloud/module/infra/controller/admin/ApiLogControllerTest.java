package com.ccloud.module.infra.controller.admin;

import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.apilog.core.service.ApiErrorLog;
import com.ccloud.module.infra.service.logger.ApiAccessLogService;
import com.ccloud.module.infra.service.logger.ApiErrorLogService;
import com.ccloud.module.infra.test.BaseInfraWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiLogControllerTest extends BaseInfraWebIntegrationTest {

    private static final String APP = "webtest-log";

    @Autowired
    private ApiErrorLogService apiErrorLogService;
    @Autowired
    private ApiAccessLogService apiAccessLogService;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM infra_api_error_log WHERE application_name = ?", APP);
        jdbcTemplate.update("DELETE FROM infra_api_access_log WHERE application_name = ?", APP);
    }

    @Test
    void 有权限的管理员可把异常日志标记为已处理_重复处理报错() throws Exception {
        Long id = insertErrorLog();

        mockMvc.perform(asAdmin(put("/admin-api/infra/api-error-log/update-status")
                        .param("id", String.valueOf(id)).param("processStatus", "1"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0));

        assertThat(jdbcTemplate.queryForMap("SELECT process_status, process_user_id FROM infra_api_error_log WHERE id = ?", id))
                .containsEntry("process_status", 1).containsEntry("process_user_id", (int) ADMIN_ID);
        mockMvc.perform(asAdmin(put("/admin-api/infra/api-error-log/update-status")
                        .param("id", String.valueOf(id)).param("processStatus", "2"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(1_002_002_001));
    }

    @Test
    void 无权限返回403_异常日志状态不变() throws Exception {
        Long id = insertErrorLog();
        mockMvc.perform(asAdmin(put("/admin-api/infra/api-error-log/update-status")
                        .param("id", String.valueOf(id)).param("processStatus", "1"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/infra/api-error-log/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/infra/api-access-log/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        assertThat(jdbcTemplate.queryForObject("SELECT process_status FROM infra_api_error_log WHERE id = ?", Integer.class, id)).isZero();
    }

    @Test
    void 分页可按应用名查到() throws Exception {
        insertErrorLog();
        ApiAccessLog accessLog = new ApiAccessLog();
        accessLog.setApplicationName(APP);
        accessLog.setRequestUrl("/admin-api/webtest/log");
        accessLog.setTenantId(TENANT);
        accessLog.setBeginTime(LocalDateTime.now());
        accessLog.setEndTime(LocalDateTime.now());
        accessLog.setDuration(1);
        apiAccessLogService.createApiAccessLog(accessLog);

        mockMvc.perform(asAdmin(get("/admin-api/infra/api-error-log/page").param("applicationName", APP), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].applicationName").value(APP))
                .andExpect(jsonPath("$.data.list[0].processStatus").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/infra/api-access-log/page").param("applicationName", APP), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].requestUrl").value("/admin-api/webtest/log"));
    }

    @Test
    void 导出Excel需要导出权限() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/infra/api-error-log/export-excel").param("applicationName", APP), ADMIN_ID))
                .andExpect(status().isOk());
        mockMvc.perform(asAdmin(get("/admin-api/infra/api-access-log/export-excel").param("applicationName", APP), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }

    private Long insertErrorLog() {
        ApiErrorLog errorLog = new ApiErrorLog();
        errorLog.setApplicationName(APP);
        errorLog.setTenantId(TENANT);
        errorLog.setExceptionName("java.lang.IllegalStateException");
        apiErrorLogService.createApiErrorLog(errorLog);
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM infra_api_error_log WHERE application_name = ?", Long.class, APP);
    }
}
