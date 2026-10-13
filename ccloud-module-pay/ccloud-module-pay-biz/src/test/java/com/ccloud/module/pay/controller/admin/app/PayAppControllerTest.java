package com.ccloud.module.pay.controller.admin.app;

import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class PayAppControllerTest extends BasePayWebIntegrationTest {

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM pay_channel WHERE app_id IN (SELECT id FROM (SELECT id FROM pay_app WHERE app_key LIKE 'webtest-%') t)");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key LIKE 'webtest-%'");
    }

    private static String body(String appKey) {
        return "{\"appKey\":\"" + appKey + "\",\"name\":\"Web测试应用\",\"status\":0}";
    }

    @Test
    void 创建后能在分页中查到() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/pay/app/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body("webtest-app")))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/pay/app/page").param("appKey", "webtest-app"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].name").value("Web测试应用"));
        mockMvc.perform(asAdmin(get("/admin-api/pay/app/list-all-simple"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void 应用标识格式不合法返回400() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/pay/app/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Bad Key")))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void 无权限返回403() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/pay/app/create"), NO_PERM_ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body("webtest-app")))
                .andExpect(jsonPath("$.code").value(403));
    }
}
