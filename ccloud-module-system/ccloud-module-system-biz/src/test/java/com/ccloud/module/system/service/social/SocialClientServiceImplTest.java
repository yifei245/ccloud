package com.ccloud.module.system.service.social;

import cn.hutool.core.net.url.UrlBuilder;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.module.system.api.social.dto.SocialWxPhoneNumberInfoRespDTO;
import com.ccloud.module.system.controller.admin.social.vo.client.SocialClientSaveReqVO;
import com.ccloud.module.system.dal.mysql.social.SocialClientMapper;
import com.ccloud.module.system.dal.redis.social.SocialAuthStateRedisDAO;
import com.ccloud.module.system.enums.social.SocialTypeEnum;
import com.ccloud.module.system.framework.social.SocialClientFactory;
import com.ccloud.module.system.test.BaseSystemDbAndRedisUnitTest;
import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import jakarta.annotation.Resource;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.chanjar.weixin.common.error.WxErrorException;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Import({SocialClientServiceImpl.class, SocialClientFactory.class, SocialAuthStateRedisDAO.class})
class SocialClientServiceImplTest extends BaseSystemDbAndRedisUnitTest {

    private static final Integer ADMIN = UserTypeEnum.ADMIN.getValue();

    @Resource
    private SocialClientService clientService;
    @Resource
    private SocialClientMapper clientMapper;
    @Resource
    private SocialAuthStateRedisDAO stateRedisDAO;

    @MockitoSpyBean
    private SocialClientFactory clientFactory;

    // ========== 管理 ==========

    @Test
    void createSocialClient_同平台同用户类型只能配置一个() {
        Long id = clientService.createSocialClient(reqVO(SocialTypeEnum.DINGTALK, "ding-key"));
        assertThat(clientMapper.selectById(id).getTenantId()).isEqualTo(TENANT_ID);

        assertServiceException(() -> clientService.createSocialClient(reqVO(SocialTypeEnum.DINGTALK, "other")),
                SOCIAL_CLIENT_UNIQUE);
        SocialClientSaveReqVO member = reqVO(SocialTypeEnum.DINGTALK, "other");
        member.setUserType(UserTypeEnum.MEMBER.getValue());
        clientService.createSocialClient(member);
    }

    @Test
    void updateSocialClient_成功与不存在() {
        Long id = clientService.createSocialClient(reqVO(SocialTypeEnum.DINGTALK, "ding-key"));
        SocialClientSaveReqVO reqVO = reqVO(SocialTypeEnum.DINGTALK, "new-key");
        reqVO.setId(id);
        clientService.updateSocialClient(reqVO);
        assertThat(clientMapper.selectById(id).getClientId()).isEqualTo("new-key");

        reqVO.setId(-1L);
        assertServiceException(() -> clientService.updateSocialClient(reqVO), SOCIAL_CLIENT_NOT_EXISTS);
    }

    @Test
    void deleteSocialClient_成功与不存在() {
        Long id = clientService.createSocialClient(reqVO(SocialTypeEnum.DINGTALK, "ding-key"));
        clientService.deleteSocialClient(id);
        assertThat(clientMapper.selectById(id)).isNull();
        assertServiceException(() -> clientService.deleteSocialClient(-1L), SOCIAL_CLIENT_NOT_EXISTS);
    }

    // ========== 授权 ==========

    @Test
    void getAuthorizeUrl_生成正确的授权地址_state存入Redis供回调校验() {
        clientService.createSocialClient(reqVO(SocialTypeEnum.DINGTALK, "ding-key"));

        String url = clientService.getAuthorizeUrl(SocialTypeEnum.DINGTALK.getType(), ADMIN,
                "https://admin.ccloud.local/social-login");

        UrlBuilder parsed = UrlBuilder.of(url);
        assertThat(parsed.getHost()).isEqualTo("oapi.dingtalk.com");
        assertThat(parsed.getQuery().get("appid")).hasToString("ding-key");
        assertThat(String.valueOf(parsed.getQuery().get("redirect_uri"))).isEqualTo("https://admin.ccloud.local/social-login");
        String state = String.valueOf(parsed.getQuery().get("state"));
        assertThat(state).isNotBlank();
        // 多实例部署下回调可能落在另一个节点，state 必须存在共享的 Redis 中
        assertThat(stateRedisDAO.containsKey(state)).isTrue();
    }

    @Test
    void getAuthorizeUrl_未配置或已关闭的客户端应拒绝() {
        assertServiceException(() -> clientService.getAuthorizeUrl(SocialTypeEnum.DINGTALK.getType(), ADMIN, "https://a/b"),
                SOCIAL_CLIENT_NOT_EXISTS);

        SocialClientSaveReqVO disabled = reqVO(SocialTypeEnum.WECHAT_OPEN, "wx-key");
        disabled.setStatus(CommonStatusEnum.DISABLE.getStatus());
        clientService.createSocialClient(disabled);
        assertServiceException(() -> clientService.getAuthorizeUrl(SocialTypeEnum.WECHAT_OPEN.getType(), ADMIN, "https://a/b"),
                SOCIAL_CLIENT_NOT_EXISTS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAuthUser_授权码换取社交用户成功() {
        clientService.createSocialClient(reqVO(SocialTypeEnum.DINGTALK, "ding-key"));
        AuthRequest request = mock(AuthRequest.class);
        doReturn(request).when(clientFactory).buildAuthRequest(any(), any());
        AuthUser user = AuthUser.builder().uuid("open-1").nickname("钉钉用户").build();
        when(request.login(any(AuthCallback.class))).thenReturn(new AuthResponse<>(2000, null, user));

        AuthUser result = clientService.getAuthUser(SocialTypeEnum.DINGTALK.getType(), ADMIN, "code-1", "state-1");

        assertThat(result.getUuid()).isEqualTo("open-1");
        verify(request).login(argThat(callback -> "code-1".equals(callback.getCode())
                && "state-1".equals(callback.getState())));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAuthUser_平台返回失败时抛授权失败() {
        clientService.createSocialClient(reqVO(SocialTypeEnum.DINGTALK, "ding-key"));
        AuthRequest request = mock(AuthRequest.class);
        doReturn(request).when(clientFactory).buildAuthRequest(any(), any());
        when(request.login(any(AuthCallback.class))).thenReturn(new AuthResponse<>(5009, "Illegal state", null));

        assertServiceException(() -> clientService.getAuthUser(SocialTypeEnum.DINGTALK.getType(), ADMIN, "c", "forged"),
                SOCIAL_USER_AUTH_FAILURE);
    }

    @Test
    void getWxMaPhoneNumberInfo_成功与失败() throws WxErrorException {
        clientService.createSocialClient(reqVO(SocialTypeEnum.WECHAT_MINI_PROGRAM, "wx-mini"));
        WxMaService wxMaService = mock(WxMaService.class, Answers.RETURNS_DEEP_STUBS);
        doReturn(wxMaService).when(clientFactory).buildWxMaService(any());
        WxMaPhoneNumberInfo info = new WxMaPhoneNumberInfo();
        info.setPhoneNumber("+86 13800000000");
        info.setPurePhoneNumber("13800000000");
        info.setCountryCode("86");
        when(wxMaService.getUserService().getPhoneNumber("ok-code")).thenReturn(info);
        when(wxMaService.getUserService().getPhoneNumber("bad-code")).thenThrow(new WxErrorException("invalid code"));

        SocialWxPhoneNumberInfoRespDTO resp = clientService.getWxMaPhoneNumberInfo(UserTypeEnum.MEMBER.getValue(), "ok-code");
        assertThat(resp.getPurePhoneNumber()).isEqualTo("13800000000");
        assertServiceException(() -> clientService.getWxMaPhoneNumberInfo(UserTypeEnum.MEMBER.getValue(), "bad-code"),
                SOCIAL_CLIENT_WEIXIN_MINI_APP_PHONE_CODE_ERROR);
    }

    static SocialClientSaveReqVO reqVO(SocialTypeEnum type, String clientId) {
        SocialClientSaveReqVO reqVO = new SocialClientSaveReqVO();
        reqVO.setName(type.getName());
        reqVO.setSocialType(type.getType());
        reqVO.setUserType(type == SocialTypeEnum.WECHAT_MINI_PROGRAM ? UserTypeEnum.MEMBER.getValue() : ADMIN);
        reqVO.setClientId(clientId);
        reqVO.setClientSecret("secret");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return reqVO;
    }
}
