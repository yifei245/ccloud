package com.ccloud.module.member.controller.app;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.infra.api.file.dto.FileCreateReqDTO;
import com.ccloud.module.member.test.BaseMemberWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AppMemberUserControllerTest extends BaseMemberWebIntegrationTest {

    private static final long MEMBER_ID = 90301L;
    private static final String ORIGINAL_AVATAR = "http://f/original.png";

    @BeforeEach
    void setUp() {
        cleanUp();
        jdbcTemplate.update("INSERT INTO member_user (id, mobile, password, status, register_ip, nickname, avatar, "
                + "experience, tenant_id) VALUES (?, '13933339301', '', 0, '127.0.0.1', '头像会员', ?, 0, ?)",
                MEMBER_ID, ORIGINAL_AVATAR, TENANT);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM member_user WHERE id = ?", MEMBER_ID);
    }

    @Test
    void 会员上传头像_文件交给infra存储_返回地址并更新头像() throws Exception {
        when(fileApi.createFile(any())).thenReturn(CommonResult.success("http://f/member-avatar.png"));
        byte[] content = {1, 2, 3, 4};

        mockMvc.perform(asMember(multipart(HttpMethod.PUT, "/app-api/member/user/update-avatar")
                        .file(new MockMultipartFile("avatarFile", "me.png", "image/png", content)), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("http://f/member-avatar.png"));

        ArgumentCaptor<FileCreateReqDTO> captor = ArgumentCaptor.forClass(FileCreateReqDTO.class);
        verify(fileApi).createFile(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo(content);
        assertThat(captor.getValue().getName()).isEqualTo("me.png");
        mockMvc.perform(asMember(get("/app-api/member/user/get"), MEMBER_ID))
                .andExpect(jsonPath("$.data.avatar").value("http://f/member-avatar.png"));
    }

    @Test
    void 文件被拒绝时返回infra错误码_头像不变() throws Exception {
        when(fileApi.createFile(any())).thenReturn(CommonResult.error(1_002_007_001, "不支持上传该类型的文件：image/svg+xml"));

        mockMvc.perform(asMember(multipart(HttpMethod.PUT, "/app-api/member/user/update-avatar")
                        .file(new MockMultipartFile("avatarFile", "x.svg", "image/svg+xml", new byte[]{1})), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(1_002_007_001));

        assertThat(jdbcTemplate.queryForObject("SELECT avatar FROM member_user WHERE id = ?", String.class, MEMBER_ID))
                .isEqualTo(ORIGINAL_AVATAR);
    }

    @Test
    void 管理员令牌不能调用会员头像上传() throws Exception {
        mockMvc.perform(asAdmin(multipart(HttpMethod.PUT, "/app-api/member/user/update-avatar")
                        .file(new MockMultipartFile("avatarFile", "me.png", "image/png", new byte[]{1})), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(0)));
        verify(fileApi, never()).createFile(any());
    }
}
