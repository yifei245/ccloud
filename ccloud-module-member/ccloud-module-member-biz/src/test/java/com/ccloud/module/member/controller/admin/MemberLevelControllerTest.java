package com.ccloud.module.member.controller.admin;

import com.ccloud.module.member.test.BaseMemberWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class MemberLevelControllerTest extends BaseMemberWebIntegrationTest {

    @AfterEach
    void cleanLevels() {
        jdbcTemplate.update("DELETE FROM member_level WHERE level >= 90");
    }

    @Test
    void 有权限的管理员可创建并分页查询() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/member/level/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试等级\",\"level\":91,\"experience\":910000,\"discountPercent\":90,\"status\":0}"))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/member/level/page").param("name", "测试等级"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].level").value(91))
                .andExpect(jsonPath("$.data.list[0].discountPercent").value(90));
        mockMvc.perform(asAdmin(get("/admin-api/member/level/list-all-simple"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.level == 91)].name").value("测试等级"));
    }

    @Test
    void 参数校验_折扣超过100() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/member/level/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试等级\",\"level\":92,\"experience\":920000,\"discountPercent\":101,\"status\":0}"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void 无权限的管理员返回403() throws Exception {
        mockMvc.perform(asAdmin(delete("/admin-api/member/level/delete").param("id", "1"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void 会员令牌访问管理端返回401() throws Exception {
        mockMvc.perform(asMember(get("/admin-api/member/level/page"), 90101L))
                .andExpect(jsonPath("$.code").value(401));
    }
}
