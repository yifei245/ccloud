package com.ccloud.module.open.controller.admin.app;

import com.ccloud.module.open.test.BaseOpenWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class OpenAppControllerTest extends BaseOpenWebIntegrationTest {

    private static final Pattern MASKED = Pattern.compile("[A-Za-z0-9]{4}\\*{4}[A-Za-z0-9]{4}");

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM open_app_api WHERE app_id IN (SELECT id FROM (SELECT id FROM open_app WHERE name LIKE 'Web测试%') t)");
        jdbcTemplate.update("DELETE FROM open_app WHERE name LIKE 'Web测试%'");
        jdbcTemplate.update("DELETE FROM open_api WHERE code LIKE 'webtest.%'");
    }

    @Test
    void 创建返回32位密钥_详情与分页中的密钥为掩码且不含完整密钥() throws Exception {
        String createJson = mockMvc.perform(asAdmin(post("/admin-api/open/app/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body(null, "Web测试a")))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.appKey", matchesPattern("[a-z0-9]{16}")))
                .andExpect(jsonPath("$.data.appSecret", matchesPattern("[A-Za-z0-9]{32}")))
                .andReturn().getResponse().getContentAsString();
        String id = extract(createJson, "\"id\":(\\d+)");
        String secret = extract(createJson, "\"appSecret\":\"([A-Za-z0-9]{32})\"");

        String getJson = mockMvc.perform(asAdmin(get("/admin-api/open/app/get").param("id", id), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("Web测试a"))
                .andExpect(jsonPath("$.data.appSecret", matchesPattern(MASKED.pattern())))
                .andReturn().getResponse().getContentAsString();
        assertThat(getJson).doesNotContain(secret);
        assertThat(extract(getJson, "\"appSecret\":\"([^\"]+)\"")).isEqualTo(secret.substring(0, 4) + "****" + secret.substring(28));

        String pageJson = mockMvc.perform(asAdmin(get("/admin-api/open/app/page").param("name", "Web测试"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].appSecret", matchesPattern(MASKED.pattern())))
                .andReturn().getResponse().getContentAsString();
        assertThat(pageJson).doesNotContain(secret);
    }

    @Test
    void 重置密钥后掩码随之改变_新密钥只在重置时返回() throws Exception {
        String createJson = mockMvc.perform(asAdmin(post("/admin-api/open/app/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body(null, "Web测试b")))
                .andReturn().getResponse().getContentAsString();
        String id = extract(createJson, "\"id\":(\\d+)");
        String oldSecret = extract(createJson, "\"appSecret\":\"([A-Za-z0-9]{32})\"");
        String oldMask = extract(mockMvc.perform(asAdmin(get("/admin-api/open/app/get").param("id", id), ADMIN_ID))
                .andReturn().getResponse().getContentAsString(), "\"appSecret\":\"([^\"]+)\"");

        String resetJson = mockMvc.perform(asAdmin(put("/admin-api/open/app/reset-secret").param("id", id), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", matchesPattern("[A-Za-z0-9]{32}")))
                .andReturn().getResponse().getContentAsString();
        String newSecret = extract(resetJson, "\"data\":\"([A-Za-z0-9]{32})\"");
        assertThat(newSecret).isNotEqualTo(oldSecret);

        String getJson = mockMvc.perform(asAdmin(get("/admin-api/open/app/get").param("id", id), ADMIN_ID))
                .andReturn().getResponse().getContentAsString();
        String newMask = extract(getJson, "\"appSecret\":\"([^\"]+)\"");
        assertThat(newMask).isEqualTo(newSecret.substring(0, 4) + "****" + newSecret.substring(28)).isNotEqualTo(oldMask);
        assertThat(getJson).doesNotContain(newSecret).doesNotContain(oldSecret);
    }

    @Test
    void 授权接口_整体替换_查询已授权编号() throws Exception {
        String appId = extract(mockMvc.perform(asAdmin(post("/admin-api/open/app/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body(null, "Web测试c")))
                .andReturn().getResponse().getContentAsString(), "\"id\":(\\d+)");
        String apiId = createApi("webtest.c", "/open-api/webtest/c");
        mockMvc.perform(asAdmin(put("/admin-api/open/app/assign-api"), ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":" + appId + ",\"apiIds\":[" + apiId + "]}"))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/open/app/get-api-ids").param("id", appId), ADMIN_ID))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0]").value(Long.parseLong(apiId)));
        mockMvc.perform(asAdmin(put("/admin-api/open/app/assign-api"), ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":" + appId + ",\"apiIds\":[]}"))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/open/app/get-api-ids").param("id", appId), ADMIN_ID))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void 修改与删除() throws Exception {
        String id = extract(mockMvc.perform(asAdmin(post("/admin-api/open/app/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body(null, "Web测试d")))
                .andReturn().getResponse().getContentAsString(), "\"id\":(\\d+)");
        mockMvc.perform(asAdmin(put("/admin-api/open/app/update"), ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content(body(id, "Web测试d2")))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/open/app/get").param("id", id), ADMIN_ID))
                .andExpect(jsonPath("$.data.name").value("Web测试d2"));
        mockMvc.perform(asAdmin(delete("/admin-api/open/app/delete").param("id", id), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/open/app/get").param("id", id), ADMIN_ID))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 参数校验() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/open/app/create"), ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Web测试e\",\"status\":0,\"rateLimit\":0}"))
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(asAdmin(post("/admin-api/open/app/create"), ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Web测试e\",\"status\":5,\"rateLimit\":10}"))
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(asAdmin(post("/admin-api/open/app/create"), ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Web测试e\",\"status\":0,\"rateLimit\":10,\"ipWhitelist\":\"example.com\"}"))
                .andExpect(jsonPath("$.code").value(1_005_001_001));
        mockMvc.perform(asAdmin(get("/admin-api/open/app/page").param("name", "Web测试e"), ADMIN_ID))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void 租户2的管理员看不到租户1的应用() throws Exception {
        String id = extract(mockMvc.perform(asAdmin(post("/admin-api/open/app/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body(null, "Web测试f")))
                .andReturn().getResponse().getContentAsString(), "\"id\":(\\d+)");
        mockMvc.perform(asAdminOfTenant(get("/admin-api/open/app/page").param("name", "Web测试"), ADMIN_ID, TENANT_2))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(asAdminOfTenant(get("/admin-api/open/app/get").param("id", id), ADMIN_ID, TENANT_2))
                .andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(asAdminOfTenant(put("/admin-api/open/app/reset-secret").param("id", id), ADMIN_ID, TENANT_2))
                .andExpect(jsonPath("$.code").value(1_005_001_000));
    }

    @Test
    void 无权限管理员返回403() throws Exception {
        mockMvc.perform(asAdmin(put("/admin-api/open/app/assign-api"), NO_PERM_ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":1,\"apiIds\":[]}"))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(post("/admin-api/open/app/create"), NO_PERM_ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content(body(null, "Web测试g")))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/open/app/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(put("/admin-api/open/app/reset-secret").param("id", "1"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }

    private String createApi(String code, String path) throws Exception {
        return extract(mockMvc.perform(asAdmin(post("/admin-api/open/api/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Web测试\",\"code\":\"" + code + "\",\"method\":\"GET\",\"path\":\"" + path + "\",\"status\":0}"))
                .andReturn().getResponse().getContentAsString(), "\"data\":(\\d+)");
    }

    private static String body(String id, String name) {
        return "{" + (id != null ? "\"id\":" + id + "," : "") + "\"name\":\"" + name + "\",\"status\":0,\"rateLimit\":100}";
    }

    private static String extract(String json, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(json);
        assertThat(matcher.find()).as(json).isTrue();
        return matcher.group(1);
    }
}
