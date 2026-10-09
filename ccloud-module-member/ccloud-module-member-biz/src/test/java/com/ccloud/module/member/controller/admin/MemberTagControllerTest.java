package com.ccloud.module.member.controller.admin;

import com.ccloud.module.member.test.BaseMemberWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class MemberTagControllerTest extends BaseMemberWebIntegrationTest {

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM member_tag WHERE name LIKE 'Web测试%'");
    }

    @Test
    void 有权限可创建_精简列表登录即可访问() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/member/tag/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Web测试标签\"}"))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/member/tag/list-all-simple"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.name == 'Web测试标签')]").isNotEmpty());
        mockMvc.perform(asAdmin(get("/admin-api/member/tag/page").param("name", "Web测试"), ADMIN_ID))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void 无权限返回403_名称为空返回400() throws Exception {
        mockMvc.perform(asAdmin(delete("/admin-api/member/tag/delete").param("id", "1"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/member/tag/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(post("/admin-api/member/tag/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(jsonPath("$.code").value(400));
    }
}
