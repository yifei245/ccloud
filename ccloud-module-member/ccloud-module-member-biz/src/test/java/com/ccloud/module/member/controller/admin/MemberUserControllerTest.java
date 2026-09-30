package com.ccloud.module.member.controller.admin;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.module.member.test.BaseMemberWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class MemberUserControllerTest extends BaseMemberWebIntegrationTest {

    private static final long MEMBER_ID = 90241L;
    private static final long BRONZE_ID = 9011L;
    private static final long SILVER_ID = 9012L;
    private static final long GROUP_ID = 9013L;

    @BeforeEach
    void setUp() {
        cleanUp();
        jdbcTemplate.update("INSERT INTO member_level (id, name, level, experience, discount_percent, status, tenant_id) "
                + "VALUES (?, '测试铜', 90, 0, 100, 0, ?), (?, '测试银', 91, 100, 98, 0, ?)",
                BRONZE_ID, TENANT, SILVER_ID, TENANT);
        jdbcTemplate.update("INSERT INTO member_group (id, name, status, tenant_id) VALUES (?, 'Web测试分组', 0, ?)",
                GROUP_ID, TENANT);
        jdbcTemplate.update("INSERT INTO member_user (id, mobile, password, status, register_ip, nickname, point, "
                + "experience, level_id, group_id, tenant_id) VALUES (?, '13933339241', '$2a$04$hash', 0, '127.0.0.1', "
                + "'会员丁', 100, 0, ?, ?, ?)", MEMBER_ID, BRONZE_ID, GROUP_ID, TENANT);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM member_point_record WHERE user_id = ?", MEMBER_ID);
        jdbcTemplate.update("DELETE FROM member_experience_record WHERE user_id = ?", MEMBER_ID);
        jdbcTemplate.update("DELETE FROM member_level_record WHERE user_id = ?", MEMBER_ID);
        jdbcTemplate.update("DELETE FROM member_user WHERE id = ?", MEMBER_ID);
        jdbcTemplate.update("DELETE FROM member_group WHERE id = ?", GROUP_ID);
        jdbcTemplate.update("DELETE FROM member_level WHERE level >= 90");
    }

    @Test
    void 详情带等级与分组名称_不返回密码_分页可按手机号查() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/member/user/get").param("id", String.valueOf(MEMBER_ID)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.levelName").value("测试铜"))
                .andExpect(jsonPath("$.data.groupName").value("Web测试分组"))
                .andExpect(jsonPath("$.data.tagIds").isEmpty())
                .andExpect(jsonPath("$.data.password").doesNotExist());
        mockMvc.perform(asAdmin(get("/admin-api/member/user/page").param("mobile", "13933339241"), ADMIN_ID))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].levelName").value("测试铜"));
    }

    @Test
    void 修改积分与等级() throws Exception {
        mockMvc.perform(asAdmin(put("/admin-api/member/user/update-point"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"id\":" + MEMBER_ID + ",\"point\":50}"))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(put("/admin-api/member/user/update-level"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + MEMBER_ID + ",\"levelId\":" + SILVER_ID + ",\"reason\":\"活动赠送\"}"))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(asAdmin(get("/admin-api/member/user/get").param("id", String.valueOf(MEMBER_ID)), ADMIN_ID))
                .andExpect(jsonPath("$.data.point").value(150))
                .andExpect(jsonPath("$.data.experience").value(100))
                .andExpect(jsonPath("$.data.levelName").value("测试银"));
    }

    @Test
    void 修改资料_禁用时作废会员令牌() throws Exception {
        mockMvc.perform(asAdmin(put("/admin-api/member/user/update"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + MEMBER_ID + ",\"nickname\":\"改名\",\"status\":1,\"groupId\":" + GROUP_ID + "}"))
                .andExpect(jsonPath("$.code").value(0));

        verify(oauth2TokenApi).removeAccessTokensByUser(MEMBER_ID, UserTypeEnum.MEMBER.getValue());
        mockMvc.perform(asAdmin(get("/admin-api/member/user/get").param("id", String.valueOf(MEMBER_ID)), ADMIN_ID))
                .andExpect(jsonPath("$.data.nickname").value("改名"))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    @Test
    void 删除会员并作废令牌() throws Exception {
        mockMvc.perform(asAdmin(delete("/admin-api/member/user/delete").param("id", String.valueOf(MEMBER_ID)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0));

        verify(oauth2TokenApi).removeAccessTokensByUser(MEMBER_ID, UserTypeEnum.MEMBER.getValue());
        mockMvc.perform(asAdmin(get("/admin-api/member/user/get").param("id", String.valueOf(MEMBER_ID)), ADMIN_ID))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 无权限返回403_会员身份返回401_参数校验() throws Exception {
        mockMvc.perform(asAdmin(put("/admin-api/member/user/update-point"), NO_PERM_ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"id\":" + MEMBER_ID + ",\"point\":50}"))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/member/user/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asMember(get("/admin-api/member/user/page"), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(asAdmin(put("/admin-api/member/user/update-level"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + MEMBER_ID + ",\"levelId\":" + SILVER_ID + ",\"reason\":\"\"}"))
                .andExpect(jsonPath("$.code").value(400));
    }
}
