package com.ccloud.module.member.controller.app;

import com.ccloud.module.member.test.BaseMemberWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AppMemberSignInControllerTest extends BaseMemberWebIntegrationTest {

    private static final long MEMBER_ID = 90221L;

    @BeforeEach
    void setUp() {
        cleanUp();
        jdbcTemplate.update("INSERT INTO member_user (id, mobile, password, status, register_ip, nickname, tenant_id) "
                + "VALUES (?, '13933339221', '', 0, '127.0.0.1', '会员丙', ?)", MEMBER_ID, TENANT);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM member_sign_in_record WHERE user_id = ?", MEMBER_ID);
        jdbcTemplate.update("DELETE FROM member_point_record WHERE user_id = ?", MEMBER_ID);
        jdbcTemplate.update("DELETE FROM member_experience_record WHERE user_id = ?", MEMBER_ID);
        jdbcTemplate.update("DELETE FROM member_user WHERE id = ?", MEMBER_ID);
        jdbcTemplate.update("DELETE FROM member_sign_in_config WHERE day >= 90");
    }

    @Test
    void 签到后概况显示今日已签_再签报错() throws Exception {
        mockMvc.perform(asMember(post("/app-api/member/sign-in/record/create"), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.day").value(1));
        mockMvc.perform(asMember(get("/app-api/member/sign-in/record/get-summary"), MEMBER_ID))
                .andExpect(jsonPath("$.data.todaySignIn").value(true))
                .andExpect(jsonPath("$.data.continuousDay").value(1))
                .andExpect(jsonPath("$.data.totalDay").value(1));
        mockMvc.perform(asMember(post("/app-api/member/sign-in/record/create"), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(1_004_010_000));
        mockMvc.perform(asMember(get("/app-api/member/sign-in/record/page"), MEMBER_ID))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void 未登录签到返回401() throws Exception {
        mockMvc.perform(post("/app-api/member/sign-in/record/create").header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 管理端签到规则_有权限可创建_用户端只看启用的() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/member/sign-in/config/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"day\":91,\"point\":9,\"experience\":1,\"status\":0}"))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(post("/admin-api/member/sign-in/config/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"day\":92,\"point\":9,\"experience\":1,\"status\":1}"))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asMember(get("/app-api/member/sign-in/config/list"), MEMBER_ID))
                .andExpect(jsonPath("$.data[?(@.day == 91)].point").value(9))
                .andExpect(jsonPath("$.data[?(@.day == 92)]").isEmpty());
        mockMvc.perform(asAdmin(get("/admin-api/member/sign-in/config/list"), ADMIN_ID))
                .andExpect(jsonPath("$.data[?(@.day == 92)].status").value(1));
    }

    @Test
    void 管理端接口无权限返回403() throws Exception {
        mockMvc.perform(asAdmin(delete("/admin-api/member/sign-in/config/delete").param("id", "1"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/member/sign-in/record/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/member/sign-in/record/page")
                        .param("userId", String.valueOf(MEMBER_ID)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void 参数校验_天数必须大于0() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/member/sign-in/config/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"day\":0,\"point\":9,\"experience\":1,\"status\":0}"))
                .andExpect(jsonPath("$.code").value(400));
    }
}
