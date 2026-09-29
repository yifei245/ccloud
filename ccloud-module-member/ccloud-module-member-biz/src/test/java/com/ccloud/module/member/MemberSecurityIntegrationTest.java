package com.ccloud.module.member;

import com.ccloud.framework.common.enums.TerminalEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.member.test.BaseMemberWebIntegrationTest;
import com.ccloud.module.system.api.oauth2.dto.OAuth2AccessTokenCreateReqDTO;
import com.ccloud.module.system.api.oauth2.dto.OAuth2AccessTokenRespDTO;
import com.ccloud.module.system.enums.oauth2.OAuth2ClientConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 从 HTTP 入口验证 C 端的鉴权边界：哪些接口可匿名、身份的用户类型、请求头的解析
 */
class MemberSecurityIntegrationTest extends BaseMemberWebIntegrationTest {

    private static final long MEMBER_ID = 90101L;

    @BeforeEach
    void setUp() {
        cleanUp();
        jdbcTemplate.update("INSERT INTO member_user (id, mobile, password, status, register_ip, nickname, tenant_id) "
                + "VALUES (?, '13933330001', '$2a$04$hash', 0, '127.0.0.1', '会员甲', ?)", MEMBER_ID, TENANT);
        when(smsCodeApi.useSmsCode(any())).thenReturn(CommonResult.success(true));
        when(loginLogApi.createLoginLog(any())).thenReturn(CommonResult.success(true));
        when(oauth2TokenApi.createAccessToken(any())).thenAnswer(invocation -> {
            OAuth2AccessTokenCreateReqDTO req = invocation.getArgument(0);
            OAuth2AccessTokenRespDTO dto = new OAuth2AccessTokenRespDTO();
            dto.setAccessToken("at-" + req.getClientId());
            dto.setUserId(req.getUserId());
            dto.setUserType(req.getUserType());
            return CommonResult.success(dto);
        });
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM member_user WHERE id = ? OR mobile LIKE '139333300%'", MEMBER_ID);
    }

    // ========== 需要登录的接口 ==========

    @Test
    void 匿名访问需登录的认证接口应返回401() throws Exception {
        mockMvc.perform(post("/app-api/member/auth/update-password").header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"1234\",\"password\":\"new123\"}"))
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/app-api/member/auth/get-permission-info").header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 匿名访问用户接口应返回401() throws Exception {
        mockMvc.perform(get("/app-api/member/user/get").header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 管理员身份访问会员接口应返回401_不能冒充同编号的会员() throws Exception {
        mockMvc.perform(asUser(get("/app-api/member/user/get"), MEMBER_ID, UserTypeEnum.ADMIN))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 会员身份获取本人信息_不返回密码() throws Exception {
        mockMvc.perform(asUser(get("/app-api/member/user/get"), MEMBER_ID, UserTypeEnum.MEMBER))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(MEMBER_ID))
                .andExpect(jsonPath("$.data.mobile").value("13933330001"))
                .andExpect(jsonPath("$.data.passwordSet").value(true))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    // ========== 匿名接口与请求头 ==========

    @Test
    void 短信登录可匿名调用_客户端与终端取自请求头() throws Exception {
        mockMvc.perform(post("/app-api/member/auth/sms-login").header("X-Tenant-Id", TENANT)
                        .header(OAuth2ClientConstants.HEADER_CLIENT_ID, OAuth2ClientConstants.CLIENT_ID_H5)
                        .header("X-Terminal", TerminalEnum.H5.getTerminal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"13933330002\",\"code\":\"1234\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("at-ccloud-h5"))
                .andExpect(jsonPath("$.data.userType").value(UserTypeEnum.MEMBER.getValue()));

        assertThat(jdbcTemplate.queryForObject("SELECT register_terminal FROM member_user WHERE mobile = '13933330002'",
                Integer.class)).isEqualTo(TerminalEnum.H5.getTerminal());
    }

    @Test
    void 未指定客户端时默认ccloud_app() throws Exception {
        mockMvc.perform(post("/app-api/member/auth/sms-login").header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"13933330003\",\"code\":\"1234\"}"))
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<OAuth2AccessTokenCreateReqDTO> captor = ArgumentCaptor.forClass(OAuth2AccessTokenCreateReqDTO.class);
        verify(oauth2TokenApi).createAccessToken(captor.capture());
        assertThat(captor.getValue().getClientId()).isEqualTo(OAuth2ClientConstants.CLIENT_ID_APP);
    }

    @Test
    void 指定管理后台客户端应拒绝() throws Exception {
        mockMvc.perform(post("/app-api/member/auth/sms-login").header("X-Tenant-Id", TENANT)
                        .header(OAuth2ClientConstants.HEADER_CLIENT_ID, OAuth2ClientConstants.CLIENT_ID_WEB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"13933330004\",\"code\":\"1234\"}"))
                .andExpect(jsonPath("$.code").value(400));
        verify(oauth2TokenApi, never()).createAccessToken(any());
    }

    @Test
    void 登出可匿名调用_作废请求携带的令牌() throws Exception {
        when(oauth2TokenApi.removeAccessToken("at-1")).thenReturn(CommonResult.success(null));

        mockMvc.perform(post("/app-api/member/auth/logout").header("X-Tenant-Id", TENANT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer at-1"))
                .andExpect(jsonPath("$.code").value(0));

        verify(oauth2TokenApi).removeAccessToken("at-1");
    }

    // ========== RPC ==========

    @Test
    void RPC获取会员用户不应返回密码() throws Exception {
        mockMvc.perform(get("/rpc-api/member/user/get").param("id", String.valueOf(MEMBER_ID))
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("会员甲"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }
}
