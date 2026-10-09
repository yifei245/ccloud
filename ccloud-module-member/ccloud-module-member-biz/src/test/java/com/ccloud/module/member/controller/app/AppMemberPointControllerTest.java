package com.ccloud.module.member.controller.app;

import com.ccloud.module.member.test.BaseMemberWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AppMemberPointControllerTest extends BaseMemberWebIntegrationTest {

    private static final long MEMBER_ID = 90211L;
    private static final long OTHER_ID = 90212L;

    @BeforeEach
    void setUp() {
        cleanUp();
        for (long id : new long[]{MEMBER_ID, OTHER_ID}) {
            jdbcTemplate.update("INSERT INTO member_user (id, mobile, password, status, register_ip, nickname, point, "
                    + "tenant_id) VALUES (?, ?, '', 0, '127.0.0.1', '会员', 50, ?)", id, "139333" + id, TENANT);
        }
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM member_point_record WHERE user_id IN (?, ?)", MEMBER_ID, OTHER_ID);
        jdbcTemplate.update("DELETE FROM member_user WHERE id IN (?, ?)", MEMBER_ID, OTHER_ID);
    }

    @Test
    void 内部接口加积分_用户端只看到本人记录() throws Exception {
        for (long id : new long[]{MEMBER_ID, OTHER_ID}) {
            mockMvc.perform(put("/rpc-api/member/point/add").header("X-Tenant-Id", TENANT)
                            .param("userId", String.valueOf(id)).param("point", "7")
                            .param("bizType", "10").param("bizId", "order-1"))
                    .andExpect(jsonPath("$.code").value(0));
        }
        mockMvc.perform(asMember(get("/app-api/member/point/record/page").param("addStatus", "true"), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].point").value(7))
                .andExpect(jsonPath("$.data.list[0].title").value("订单奖励"));
    }

    @Test
    void 内部接口扣积分_余额不足() throws Exception {
        mockMvc.perform(put("/rpc-api/member/point/reduce").header("X-Tenant-Id", TENANT)
                        .param("userId", String.valueOf(MEMBER_ID)).param("point", "51")
                        .param("bizType", "12").param("bizId", "order-1"))
                .andExpect(jsonPath("$.code").value(1_004_001_003));
    }

    @Test
    void 管理端积分记录分页_权限() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/member/point/record/page")
                        .param("userId", String.valueOf(MEMBER_ID)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/member/point/record/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void 未登录查积分记录返回401() throws Exception {
        mockMvc.perform(get("/app-api/member/point/record/page").header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(401));
    }
}
