package com.ccloud.module.system.service.social;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.module.system.api.social.dto.SocialUserBindReqDTO;
import com.ccloud.module.system.api.social.dto.SocialUserRespDTO;
import com.ccloud.module.system.api.social.dto.SocialUserUnbindReqDTO;
import com.ccloud.module.system.dal.dataobject.social.SocialUserDO;
import com.ccloud.module.system.dal.mysql.social.SocialUserMapper;
import com.ccloud.module.system.enums.social.SocialTypeEnum;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.ccloud.module.system.enums.ErrorCodeConstants.SOCIAL_USER_BIND_DUPLICATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Import(SocialUserServiceImpl.class)
class SocialUserServiceImplTest extends BaseSystemDbUnitTest {

    private static final Integer ADMIN = UserTypeEnum.ADMIN.getValue();
    private static final Integer DING = SocialTypeEnum.DINGTALK.getType();

    @Resource
    private SocialUserService socialUserService;
    @Resource
    private SocialUserMapper socialUserMapper;
    @Resource
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @MockitoBean
    private SocialClientService socialClientService;

    @Test
    void authSocialUser_换取成功后落库_同一code与state复用不再请求平台() {
        mockAuthUser("code-1", "open-1", "钉钉用户");

        SocialUserDO first = socialUserService.authSocialUser(DING, ADMIN, "code-1", "state-1");
        SocialUserDO second = socialUserService.authSocialUser(DING, ADMIN, "code-1", "state-1");

        assertThat(first.getOpenid()).isEqualTo("open-1");
        assertThat(first.getNickname()).isEqualTo("钉钉用户");
        assertThat(first.getToken()).isEqualTo("token-code-1");
        assertThat(first.getRawUserInfo()).contains("钉钉用户");
        assertThat(socialUserMapper.selectById(first.getId()).getTenantId()).isEqualTo(TENANT_ID);
        assertThat(second.getId()).isEqualTo(first.getId());
        // 授权码只能用一次：先登录、再绑定两步用同一个 code 时，第二步必须走本地记录
        verify(socialClientService, times(1)).getAuthUser(any(), any(), any(), any());
    }

    @Test
    void authSocialUser_超过复用时限的旧授权码必须重新向平台校验_防止重放() {
        mockAuthUser("code-1", "open-1", "钉钉用户");
        SocialUserDO first = socialUserService.authSocialUser(DING, ADMIN, "code-1", "state-1");
        jdbcTemplate.update("UPDATE system_social_user SET update_time = ? WHERE id = ?",
                java.time.LocalDateTime.now().minusMinutes(10), first.getId());
        when(socialClientService.getAuthUser(eq(DING), eq(ADMIN), eq("code-1"), anyString()))
                .thenThrow(com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception(
                        com.ccloud.module.system.enums.ErrorCodeConstants.SOCIAL_USER_AUTH_FAILURE, "code 已使用"));

        assertServiceException(() -> socialUserService.authSocialUser(DING, ADMIN, "code-1", "state-1"),
                com.ccloud.module.system.enums.ErrorCodeConstants.SOCIAL_USER_AUTH_FAILURE);
    }

    @Test
    void authSocialUser_同一社交账号再次授权时更新记录而不是新增() {
        mockAuthUser("code-1", "open-1", "旧昵称");
        mockAuthUser("code-2", "open-1", "新昵称");

        SocialUserDO first = socialUserService.authSocialUser(DING, ADMIN, "code-1", "s");
        SocialUserDO second = socialUserService.authSocialUser(DING, ADMIN, "code-2", "s");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(socialUserMapper.selectById(first.getId()).getNickname()).isEqualTo("新昵称");
    }

    @Test
    void 已绑定时返回用户编号_未绑定时返回null() {
        mockAuthUser("code-1", "open-1", "a");
        mockAuthUser("code-2", "open-2", "b");
        socialUserService.bindSocialUser(bindReqDTO(1L, "code-1"));

        assertThat(socialUserService.getSocialUserByCode(ADMIN, DING, "code-1", "state").getUserId()).isEqualTo(1L);
        assertThat(socialUserService.getSocialUserByCode(ADMIN, DING, "code-2", "state").getUserId()).isNull();
    }

    @Test
    void bindSocialUser_社交账号已绑定其他用户时应失败() {
        mockAuthUser("code-1", "open-1", "a");
        socialUserService.bindSocialUser(bindReqDTO(1L, "code-1"));

        assertServiceException(() -> socialUserService.bindSocialUser(bindReqDTO(2L, "code-1")), SOCIAL_USER_BIND_DUPLICATE);
        // 同一用户重复绑定同一账号是幂等的
        assertThat(socialUserService.bindSocialUser(bindReqDTO(1L, "code-1"))).isEqualTo("open-1");
    }

    @Test
    void bindSocialUser_同一用户换绑同平台另一账号时替换旧绑定() {
        mockAuthUser("code-1", "open-1", "a");
        mockAuthUser("code-2", "open-2", "b");
        socialUserService.bindSocialUser(bindReqDTO(1L, "code-1"));
        socialUserService.bindSocialUser(bindReqDTO(1L, "code-2"));

        SocialUserRespDTO bound = socialUserService.getSocialUserByUserId(ADMIN, 1L, DING);
        assertThat(bound.getOpenid()).isEqualTo("open-2");
        assertThat(socialUserService.getSocialUserByCode(ADMIN, DING, "code-1", "state").getUserId()).isNull();
    }

    @Test
    void unbindSocialUser_解绑后查不到绑定() {
        mockAuthUser("code-1", "open-1", "a");
        socialUserService.bindSocialUser(bindReqDTO(1L, "code-1"));

        SocialUserUnbindReqDTO reqDTO = new SocialUserUnbindReqDTO();
        reqDTO.setUserId(1L);
        reqDTO.setUserType(ADMIN);
        reqDTO.setSocialType(DING);
        reqDTO.setOpenid("open-1");
        socialUserService.unbindSocialUser(reqDTO);

        assertThat(socialUserService.getSocialUserByUserId(ADMIN, 1L, DING)).isNull();
    }

    private void mockAuthUser(String code, String openid, String nickname) {
        JSONObject raw = new JSONObject();
        raw.put("nick", nickname);
        AuthUser user = AuthUser.builder().uuid(openid).nickname(nickname).avatar("https://a/avatar.png")
                .token(AuthToken.builder().accessToken("token-" + code).build()).rawUserInfo(raw).build();
        when(socialClientService.getAuthUser(eq(DING), eq(ADMIN), eq(code), anyString())).thenReturn(user);
    }

    private static SocialUserBindReqDTO bindReqDTO(Long userId, String code) {
        SocialUserBindReqDTO reqDTO = new SocialUserBindReqDTO();
        reqDTO.setUserId(userId);
        reqDTO.setUserType(ADMIN);
        reqDTO.setSocialType(DING);
        reqDTO.setCode(code);
        reqDTO.setState("state");
        return reqDTO;
    }
}
