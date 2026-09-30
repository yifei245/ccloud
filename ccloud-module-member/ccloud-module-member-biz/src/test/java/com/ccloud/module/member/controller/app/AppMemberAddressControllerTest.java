package com.ccloud.module.member.controller.app;

import com.ccloud.module.member.test.BaseMemberWebIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AppMemberAddressControllerTest extends BaseMemberWebIntegrationTest {

    private static final long MEMBER_ID = 90231L;
    private static final long OTHER_ID = 90232L;
    private static final String BODY = "{\"name\":\"张三\",\"mobile\":\"13800138000\",\"areaId\":440305,"
            + "\"detailAddress\":\"科技园 1 号\",\"defaultStatus\":false}";

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM member_address WHERE user_id IN (?, ?)", MEMBER_ID, OTHER_ID);
    }

    @Test
    void 创建后本人可查_他人查询返回不存在_内部接口按用户过滤() throws Exception {
        String response = mockMvc.perform(asMember(post("/app-api/member/address/create"), MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(response, "$.data")).longValue();

        mockMvc.perform(asMember(get("/app-api/member/address/list"), MEMBER_ID))
                .andExpect(jsonPath("$.data[0].id").value(id))
                .andExpect(jsonPath("$.data[0].defaultStatus").value(true));
        mockMvc.perform(asMember(get("/app-api/member/address/get-default"), MEMBER_ID))
                .andExpect(jsonPath("$.data.id").value(id));
        mockMvc.perform(asMember(get("/app-api/member/address/get").param("id", String.valueOf(id)), OTHER_ID))
                .andExpect(jsonPath("$.code").value(1_004_004_000));

        mockMvc.perform(get("/rpc-api/member/address/get").header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(id)).param("userId", String.valueOf(MEMBER_ID)))
                .andExpect(jsonPath("$.data.detailAddress").value("科技园 1 号"));
        mockMvc.perform(get("/rpc-api/member/address/get").header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(id)).param("userId", String.valueOf(OTHER_ID)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(get("/rpc-api/member/address/get-default").header("X-Tenant-Id", TENANT)
                        .param("userId", String.valueOf(MEMBER_ID)))
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    void 参数校验_手机号格式() throws Exception {
        mockMvc.perform(asMember(post("/app-api/member/address/create"), MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("13800138000", "123")))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void 未登录返回401() throws Exception {
        mockMvc.perform(get("/app-api/member/address/list").header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(401));
    }
}
