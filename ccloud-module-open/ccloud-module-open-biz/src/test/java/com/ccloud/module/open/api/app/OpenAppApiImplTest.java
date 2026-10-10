package com.ccloud.module.open.api.app;

import com.ccloud.module.open.test.BaseOpenWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class OpenAppApiImplTest extends BaseOpenWebIntegrationTest {

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM open_app_api WHERE app_id IN (SELECT id FROM (SELECT id FROM open_app WHERE name LIKE 'Web测试%') t)");
        jdbcTemplate.update("DELETE FROM open_app WHERE name LIKE 'Web测试%'");
        jdbcTemplate.update("DELETE FROM open_api WHERE code LIKE 'webtest.%'");
    }

    @Test
    void 快照含原文密钥与开启的授权接口_不带身份头_不存在返回null() throws Exception {
        String apiId = extract(mockMvc.perform(asAdmin(post("/admin-api/open/api/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Web测试\",\"code\":\"webtest.rpc\",\"method\":\"GET\",\"path\":\"/open-api/webtest/rpc\",\"status\":0}"))
                .andReturn().getResponse().getContentAsString(), "\"data\":(\\d+)");
        String created = mockMvc.perform(asAdmin(post("/admin-api/open/app/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Web测试rpc\",\"status\":0,\"rateLimit\":7,\"ipWhitelist\":\"10.0.0.0/8\"}"))
                .andReturn().getResponse().getContentAsString();
        String appId = extract(created, "\"id\":(\\d+)");
        String appKey = extract(created, "\"appKey\":\"([a-z0-9]{16})\"");
        String secret = extract(created, "\"appSecret\":\"([A-Za-z0-9]{32})\"");
        mockMvc.perform(asAdmin(put("/admin-api/open/app/assign-api"), ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":" + appId + ",\"apiIds\":[" + apiId + "]}"))
                .andExpect(jsonPath("$.code").value(0));

        // 网关调用：没有身份头，也没有租户头
        mockMvc.perform(get("/rpc-api/open/app/get-by-key").param("appKey", appKey))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(Long.parseLong(appId)))
                .andExpect(jsonPath("$.data.appKey").value(appKey))
                .andExpect(jsonPath("$.data.appSecret").value(secret))
                .andExpect(jsonPath("$.data.tenantId").value(TENANT))
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.rateLimit").value(7))
                .andExpect(jsonPath("$.data.ipWhitelist").value("10.0.0.0/8"))
                .andExpect(jsonPath("$.data.apis.length()").value(1))
                .andExpect(jsonPath("$.data.apis[0].method").value("GET"))
                .andExpect(jsonPath("$.data.apis[0].path").value("/open-api/webtest/rpc"));

        mockMvc.perform(get("/rpc-api/open/app/get-by-key").param("appKey", "not-exists"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private static String extract(String json, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(json);
        assertTrue(matcher.find(), json);
        return matcher.group(1);
    }
}
