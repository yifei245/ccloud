package com.ccloud.module.open.controller.admin.logger;

import com.ccloud.framework.excel.core.util.ExcelUtils;
import com.ccloud.module.open.controller.admin.logger.vo.OpenApiLogRespVO;
import com.ccloud.module.open.test.BaseOpenWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class OpenApiLogControllerTest extends BaseOpenWebIntegrationTest {

    @BeforeEach
    void insertLogs() {
        insertLog("webtest-ctl-1", 1L, 0);
        insertLog("webtest-ctl-2", 2L, 0);
        insertLog("webtest-ctl-3", 1L, 1_002_000_001);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM open_api_log WHERE trace_id LIKE 'webtest-ctl-%'");
    }

    @Test
    void 分页按租户隔离() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/open/api-log/page").param("apiCode", "webtest.ctl"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list", hasSize(2)))
                .andExpect(jsonPath("$.data.list[0].traceId").value("webtest-ctl-3"))
                .andExpect(jsonPath("$.data.list[1].traceId").value("webtest-ctl-1"))
                .andExpect(jsonPath("$.data.list[0].appKey").value("webtestkey000001"))
                .andExpect(jsonPath("$.data.list[0].tenantId").value(1));
        mockMvc.perform(asAdminOfTenant(get("/admin-api/open/api-log/page").param("apiCode", "webtest.ctl"), ADMIN_ID, TENANT_2))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].traceId").value("webtest-ctl-2"));
    }

    @Test
    void 分页按结果码过滤() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/open/api-log/page").param("apiCode", "webtest.ctl")
                        .param("resultCode", "1002000001"), ADMIN_ID))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].traceId").value("webtest-ctl-3"));
    }

    @Test
    void 无权限管理员返回403() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/open/api-log/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/open/api-log/export-excel"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void 导出Excel_只含本租户且不含请求参数列() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(asAdmin(get("/admin-api/open/api-log/export-excel")
                        .param("apiCode", "webtest.ctl"), ADMIN_ID))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).contains("spreadsheetml");
        assertThat(response.getHeader("Content-Disposition")).contains("attachment");
        List<OpenApiLogRespVO> rows = ExcelUtils.read(new ByteArrayInputStream(response.getContentAsByteArray()),
                OpenApiLogRespVO.class);
        assertThat(rows).extracting(OpenApiLogRespVO::getTraceId).containsExactlyInAnyOrder("webtest-ctl-1", "webtest-ctl-3");
        assertThat(rows).allSatisfy(row -> assertThat(row.getRequestParams()).isNull());
    }

    private void insertLog(String traceId, long tenantId, int resultCode) {
        jdbcTemplate.update("INSERT INTO open_api_log (app_id, app_key, api_code, request_method, request_url, request_params, "
                        + "result_code, result_msg, user_ip, duration, begin_time, end_time, trace_id, tenant_id) "
                        + "VALUES (?, ?, ?, 'GET', '/open-api/webtest/ctl', '{\"a\":1}', ?, '', '10.0.0.1', 5, ?, ?, ?, ?)",
                1L, "webtestkey000001", "webtest.ctl", resultCode, LocalDateTime.now(), LocalDateTime.now(), traceId, tenantId);
    }
}
