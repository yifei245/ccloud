package com.ccloud.module.member.controller.admin;

import com.ccloud.module.member.test.BaseMemberWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class MemberGroupControllerTest extends BaseMemberWebIntegrationTest {

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM member_group WHERE name LIKE 'Web测试%'");
    }

    @Test
    void 有权限可创建_精简列表只含启用分组() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/member/group/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Web测试启用\",\"status\":0}"))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(post("/admin-api/member/group/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Web测试停用\",\"status\":1}"))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/member/group/list-all-simple"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.name == 'Web测试启用')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Web测试停用')]").isEmpty());
    }

    @Test
    void 无权限返回403_状态非法返回400() throws Exception {
        mockMvc.perform(asAdmin(delete("/admin-api/member/group/delete").param("id", "1"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(post("/admin-api/member/group/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Web测试非法\",\"status\":9}"))
                .andExpect(jsonPath("$.code").value(400));
    }
}
