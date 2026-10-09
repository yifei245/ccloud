package com.ccloud.module.infra.controller.admin;

import com.ccloud.module.infra.test.BaseInfraWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class ConfigControllerTest extends BaseInfraWebIntegrationTest {

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM infra_config WHERE config_key LIKE 'webtest.%'");
    }

    @Test
    void 创建后可按键名读取_修改后立即读到新值_缓存已清除() throws Exception {
        String id = create("webtest.a", "1", true);
        getValue("webtest.a").andExpect(jsonPath("$.data").value("1"));
        mockMvc.perform(asAdmin(put("/admin-api/infra/config/update"), ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content(body(id, "webtest.a", "2", true)))
                .andExpect(jsonPath("$.code").value(0));
        getValue("webtest.a").andExpect(jsonPath("$.data").value("2"));
        // 内部接口读到同一个值
        mockMvc.perform(get("/rpc-api/infra/config/get-value-by-key").param("key", "webtest.a").header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data").value("2"));
    }

    @Test
    void 不可见参数前端读取报错_内部接口可以读取() throws Exception {
        create("webtest.secret", "s", false);
        getValue("webtest.secret").andExpect(jsonPath("$.code").value(1_002_000_004));
        mockMvc.perform(get("/rpc-api/infra/config/get-value-by-key").param("key", "webtest.secret").header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data").value("s"));
        mockMvc.perform(get("/rpc-api/infra/config/get-value-by-key").param("key", "webtest.none").header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 无权限返回403_读取参数登录即可() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/infra/config/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/infra/config/get-value-by-key").param("key", "webtest.none"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0));
    }

    private String create(String key, String value, boolean visible) throws Exception {
        return mockMvc.perform(asAdmin(post("/admin-api/infra/config/create"), ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content(body(null, key, value, visible)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"data\":(\\d+).*", "$1");
    }

    private org.springframework.test.web.servlet.ResultActions getValue(String key) throws Exception {
        return mockMvc.perform(asAdmin(get("/admin-api/infra/config/get-value-by-key").param("key", key), ADMIN_ID));
    }

    private static String body(String id, String key, String value, boolean visible) {
        return "{" + (id != null ? "\"id\":" + id + "," : "") + "\"category\":\"webtest\",\"name\":\"Web测试\",\"key\":\"" + key
                + "\",\"value\":\"" + value + "\",\"visible\":" + visible + "}";
    }
}
