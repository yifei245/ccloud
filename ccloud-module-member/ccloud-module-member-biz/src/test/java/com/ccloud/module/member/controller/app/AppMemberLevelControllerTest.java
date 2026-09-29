package com.ccloud.module.member.controller.app;

import com.ccloud.module.member.test.BaseMemberWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AppMemberLevelControllerTest extends BaseMemberWebIntegrationTest {

    private static final long MEMBER_ID = 90201L;
    private static final long LEVEL_ID = 9001L;

    @BeforeEach
    void setUp() {
        cleanUp();
        jdbcTemplate.update("INSERT INTO member_level (id, name, level, experience, discount_percent, status, tenant_id) "
                + "VALUES (?, '测试青铜', 90, 0, 100, 0, ?)", LEVEL_ID, TENANT);
        jdbcTemplate.update("INSERT INTO member_user (id, mobile, password, status, register_ip, nickname, experience, "
                + "level_id, tenant_id) VALUES (?, '13933339001', '', 0, '127.0.0.1', '会员乙', 30, ?, ?)",
                MEMBER_ID, LEVEL_ID, TENANT);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM member_experience_record WHERE user_id = ?", MEMBER_ID);
        jdbcTemplate.update("DELETE FROM member_level_record WHERE user_id = ?", MEMBER_ID);
        jdbcTemplate.update("DELETE FROM member_user WHERE id = ?", MEMBER_ID);
        jdbcTemplate.update("DELETE FROM member_level WHERE level >= 90");
    }

    @Test
    void 我的等级与经验() throws Exception {
        mockMvc.perform(asMember(get("/app-api/member/level/get-my"), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.experience").value(30))
                .andExpect(jsonPath("$.data.level.name").value("测试青铜"));
        mockMvc.perform(asMember(get("/app-api/member/level/list"), MEMBER_ID))
                .andExpect(jsonPath("$.data[?(@.level == 90)].name").value("测试青铜"));
    }

    @Test
    void 未登录返回401() throws Exception {
        mockMvc.perform(get("/app-api/member/level/get-my").header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 内部接口加经验_同一业务编号只生效一次_用户端可查经验记录() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(put("/rpc-api/member/experience/add").header("X-Tenant-Id", TENANT)
                            .param("userId", String.valueOf(MEMBER_ID)).param("experience", "20")
                            .param("bizType", "2").param("bizId", "order-9"))
                    .andExpect(jsonPath("$.code").value(0));
        }
        assertThat(jdbcTemplate.queryForObject("SELECT experience FROM member_user WHERE id = ?", Integer.class, MEMBER_ID))
                .isEqualTo(50);
        mockMvc.perform(asMember(get("/app-api/member/experience-record/page"), MEMBER_ID))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].experience").value(20));
    }

    @Test
    void 内部接口扣经验_业务类型方向不符() throws Exception {
        mockMvc.perform(put("/rpc-api/member/experience/reduce").header("X-Tenant-Id", TENANT)
                        .param("userId", String.valueOf(MEMBER_ID)).param("experience", "20")
                        .param("bizType", "2").param("bizId", "order-9"))
                .andExpect(jsonPath("$.code").value(1_004_013_000));
    }

    @Test
    void 内部接口获得等级() throws Exception {
        mockMvc.perform(get("/rpc-api/member/level/get").header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(LEVEL_ID)))
                .andExpect(jsonPath("$.data.name").value("测试青铜"))
                .andExpect(jsonPath("$.data.discountPercent").value(100));
    }

    @Test
    void 管理端等级记录与经验记录分页_权限() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/member/experience-record/page")
                        .param("userId", String.valueOf(MEMBER_ID)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/member/level-record/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/member/experience-record/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }
}
