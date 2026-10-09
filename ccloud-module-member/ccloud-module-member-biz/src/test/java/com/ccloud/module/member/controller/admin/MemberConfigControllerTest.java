package com.ccloud.module.member.controller.admin;

import com.ccloud.module.member.test.BaseMemberWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class MemberConfigControllerTest extends BaseMemberWebIntegrationTest {

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM member_config WHERE tenant_id = ?", TENANT);
    }

    @Test
    void 有权限保存后读取一致() throws Exception {
        mockMvc.perform(asAdmin(put("/admin-api/member/config/save"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pointTradeDeductEnable\":true,\"pointTradeDeductUnitPrice\":1,"
                                + "\"pointTradeDeductMaxPrice\":10000,\"pointTradeGivePoint\":1}"))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/member/config/get"), ADMIN_ID))
                .andExpect(jsonPath("$.data.pointTradeDeductEnable").value(true))
                .andExpect(jsonPath("$.data.pointTradeDeductMaxPrice").value(10000));
    }

    @Test
    void 无权限返回403_单价为负返回400() throws Exception {
        mockMvc.perform(asAdmin(put("/admin-api/member/config/save"), NO_PERM_ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pointTradeDeductEnable\":true,\"pointTradeDeductUnitPrice\":1}"))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/member/config/get"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(put("/admin-api/member/config/save"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pointTradeDeductEnable\":true,\"pointTradeDeductUnitPrice\":-1}"))
                .andExpect(jsonPath("$.code").value(400));
    }
}
