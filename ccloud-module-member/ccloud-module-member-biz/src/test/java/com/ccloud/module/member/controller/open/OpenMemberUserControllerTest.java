package com.ccloud.module.member.controller.open;

import com.ccloud.module.member.test.BaseMemberWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class OpenMemberUserControllerTest extends BaseMemberWebIntegrationTest {

    private static final String MOBILE = "13900009901";
    private static final long MEMBER_ID = 90801L;
    private static final long LEVEL_ID = 90801L;

    @BeforeEach
    void insertMember() {
        cleanup();
        jdbcTemplate.update("INSERT INTO member_level (id, name, level, experience, discount_percent, status, "
                + "tenant_id) VALUES (?, '开放测试等级', 97, 0, 100, 0, ?)", LEVEL_ID, TENANT);
        jdbcTemplate.update("INSERT INTO member_user (id, mobile, password, status, register_ip, nickname, point, "
                + "level_id, tenant_id) VALUES (?, ?, 'secret-pwd', 0, '10.1.2.3', 'Web测试', 88, ?, ?)",
                MEMBER_ID, MOBILE, LEVEL_ID, TENANT);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM member_user WHERE mobile = ? OR id = ?", MOBILE, MEMBER_ID);
        jdbcTemplate.update("DELETE FROM member_level WHERE id = ?", LEVEL_ID);
    }

    @Test
    void 开放应用按手机号查询_手机号脱敏_不返回敏感字段() throws Exception {
        mockMvc.perform(asOpenApp(get("/open-api/member/user/get").param("mobile", MOBILE), 9L))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.mobile").value("139****9901"))
                .andExpect(jsonPath("$.data.nickname").value("Web测试"))
                .andExpect(jsonPath("$.data.point").value(88))
                .andExpect(jsonPath("$.data.levelName").isNotEmpty())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.registerIp").doesNotExist());
    }

    @Test
    void 手机号未注册返回业务错误() throws Exception {
        mockMvc.perform(asOpenApp(get("/open-api/member/user/get").param("mobile", "13900009999"), 9L))
                .andExpect(jsonPath("$.code").value(1_004_001_001));
    }

    @Test
    void 管理员与会员身份或匿名访问开放接口返回401() throws Exception {
        mockMvc.perform(asAdmin(get("/open-api/member/user/get").param("mobile", MOBILE), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(asMember(get("/open-api/member/user/get").param("mobile", MOBILE), 1L))
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/open-api/member/user/get").param("mobile", MOBILE).header("X-Tenant-Id", 1))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 开放应用身份访问用户端接口返回401() throws Exception {
        mockMvc.perform(asOpenApp(get("/app-api/member/user/get"), 9L))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 只查应用所属租户的会员() throws Exception {
        jdbcTemplate.update("UPDATE member_user SET tenant_id = 2 WHERE id = ?", MEMBER_ID);

        mockMvc.perform(asOpenApp(get("/open-api/member/user/get").param("mobile", MOBILE), 9L))
                .andExpect(jsonPath("$.code").value(1_004_001_001));
    }
}
