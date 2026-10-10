package com.ccloud.module.open.controller.admin.api;

import com.ccloud.module.open.test.BaseOpenWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class OpenApiControllerTest extends BaseOpenWebIntegrationTest {

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM open_api WHERE code LIKE 'webtest.%'");
    }

    @Test
    void 管理员创建后可查询_分页按编码过滤() throws Exception {
        String id = create("webtest.a", "GET", "/open-api/webtest/a", 0);
        mockMvc.perform(asAdmin(get("/admin-api/open/api/get").param("id", id), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.code").value("webtest.a"))
                .andExpect(jsonPath("$.data.method").value("GET"))
                .andExpect(jsonPath("$.data.path").value("/open-api/webtest/a"));
        mockMvc.perform(asAdmin(get("/admin-api/open/api/page").param("code", "webtest."), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(Long.parseLong(id)));
    }

    @Test
    void 修改与删除() throws Exception {
        String id = create("webtest.b", "get", "/open-api/webtest/b", 0);
        mockMvc.perform(asAdmin(put("/admin-api/open/api/update"), ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content(body(id, "webtest.b", "POST", "/open-api/webtest/b2", 0)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/open/api/get").param("id", id), ADMIN_ID))
                .andExpect(jsonPath("$.data.method").value("POST"))
                .andExpect(jsonPath("$.data.path").value("/open-api/webtest/b2"));
        mockMvc.perform(asAdmin(delete("/admin-api/open/api/delete").param("id", id), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/open/api/get").param("id", id), ADMIN_ID))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 无权限管理员返回403() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/open/api/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(post("/admin-api/open/api/create"), NO_PERM_ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content(body(null, "webtest.c", "GET", "/open-api/webtest/c", 0)))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/open/api/list-all-simple"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void 请求方法不是GET_POST_PUT_DELETE返回参数错误() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/open/api/create"), ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content(body(null, "webtest.d", "PATCH", "/open-api/webtest/d", 0)))
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(asAdmin(post("/admin-api/open/api/create"), ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content(body(null, "webtest.d", "GET", "/open-api/webtest/d", 5)))
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(asAdmin(get("/admin-api/open/api/page").param("code", "webtest.d"), ADMIN_ID))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void 普通租户的管理员不能维护接口目录_但可以查询() throws Exception {
        String id = create("webtest.e", "GET", "/open-api/webtest/e", 0);
        mockMvc.perform(asAdminOfTenant(post("/admin-api/open/api/create"), ADMIN_ID, TENANT_2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(null, "webtest.e2", "GET", "/open-api/webtest/e2", 0)))
                .andExpect(jsonPath("$.code").value(1_005_000_000));
        mockMvc.perform(asAdminOfTenant(put("/admin-api/open/api/update"), ADMIN_ID, TENANT_2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(id, "webtest.e", "GET", "/open-api/webtest/e3", 0)))
                .andExpect(jsonPath("$.code").value(1_005_000_000));
        mockMvc.perform(asAdminOfTenant(delete("/admin-api/open/api/delete").param("id", id), ADMIN_ID, TENANT_2))
                .andExpect(jsonPath("$.code").value(1_005_000_000));
        // 目录是全局数据，普通租户可以查询（设计：普通套餐保留接口目录查询，授权勾选需要），且数据未被改动
        mockMvc.perform(asAdminOfTenant(get("/admin-api/open/api/page").param("code", "webtest."), ADMIN_ID, TENANT_2))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].path").value("/open-api/webtest/e"));
    }

    @Test
    void 精简列表不含关闭的接口() throws Exception {
        String on = create("webtest.on", "GET", "/open-api/webtest/on", 0);
        String off = create("webtest.off", "GET", "/open-api/webtest/off", 1);
        mockMvc.perform(asAdmin(get("/admin-api/open/api/list-all-simple"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.code == 'webtest.on')].id").value(hasItem(Integer.parseInt(on))))
                .andExpect(jsonPath("$.data[?(@.code == 'webtest.off')]").isEmpty())
                .andExpect(jsonPath("$.data[?(@.code == 'webtest.on')].path").value(hasItem("/open-api/webtest/on")));
    }

    private String create(String code, String method, String path, int status) throws Exception {
        ResultActions result = mockMvc.perform(asAdmin(post("/admin-api/open/api/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body(null, code, method, path, status)))
                .andExpect(jsonPath("$.code").value(0));
        return result.andReturn().getResponse().getContentAsString().replaceAll(".*\"data\":(\\d+).*", "$1");
    }

    private static String body(String id, String code, String method, String path, int status) {
        return "{" + (id != null ? "\"id\":" + id + "," : "") + "\"name\":\"Web测试\",\"code\":\"" + code
                + "\",\"method\":\"" + method + "\",\"path\":\"" + path + "\",\"status\":" + status + "}";
    }
}
