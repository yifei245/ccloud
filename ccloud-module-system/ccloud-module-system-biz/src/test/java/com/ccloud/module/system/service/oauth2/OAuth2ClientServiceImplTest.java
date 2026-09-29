package com.ccloud.module.system.service.oauth2;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.system.controller.admin.oauth2.vo.client.OAuth2ClientPageReqVO;
import com.ccloud.module.system.controller.admin.oauth2.vo.client.OAuth2ClientSaveReqVO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import com.ccloud.module.system.dal.mysql.oauth2.OAuth2ClientMapper;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import com.ccloud.module.system.test.CacheTestConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static com.ccloud.module.system.service.oauth2.OAuth2TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import({OAuth2ClientServiceImpl.class, CacheTestConfiguration.class})
class OAuth2ClientServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private OAuth2ClientService clientService;
    @Resource
    private OAuth2ClientMapper clientMapper;

    @Test
    void createClient_成功与编号重复() {
        Long id = clientService.createOAuth2Client(saveReqVO("new-client"));
        OAuth2ClientDO client = clientMapper.selectById(id);
        assertThat(client.getRedirectUris()).containsExactly(REDIRECT_URI);
        assertThat(client.getAuthorizedGrantTypes()).contains("password");

        assertServiceException(() -> clientService.createOAuth2Client(saveReqVO("new-client")), OAUTH2_CLIENT_EXISTS);
    }

    @Test
    void updateClient_成功且清除缓存_不存在时失败() {
        clientMapper.insert(client());
        assertThat(clientService.validOAuthClientFromCache(CLIENT_ID).getName()).isEqualTo("测试客户端");

        OAuth2ClientSaveReqVO reqVO = saveReqVO(CLIENT_ID);
        reqVO.setId(clientMapper.selectByClientId(CLIENT_ID).getId());
        reqVO.setName("改名");
        clientService.updateOAuth2Client(reqVO);
        assertThat(clientService.validOAuthClientFromCache(CLIENT_ID).getName()).isEqualTo("改名");

        OAuth2ClientSaveReqVO missing = saveReqVO("missing");
        missing.setId(-1L);
        assertServiceException(() -> clientService.updateOAuth2Client(missing), OAUTH2_CLIENT_NOT_EXISTS);
    }

    @Test
    void deleteClient_成功后校验不存在() {
        clientMapper.insert(client());
        clientService.validOAuthClientFromCache(CLIENT_ID);
        clientService.deleteOAuth2Client(clientMapper.selectByClientId(CLIENT_ID).getId());
        assertServiceException(() -> clientService.validOAuthClientFromCache(CLIENT_ID), OAUTH2_CLIENT_NOT_EXISTS);
        assertServiceException(() -> clientService.deleteOAuth2Client(-1L), OAUTH2_CLIENT_NOT_EXISTS);
    }

    @Test
    void getClientPage_按条件过滤() {
        clientMapper.insert(client());
        clientMapper.insert(client().setClientId("other").setName("其他").setStatus(CommonStatusEnum.DISABLE.getStatus()));
        OAuth2ClientPageReqVO reqVO = new OAuth2ClientPageReqVO();
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        assertThat(clientService.getOAuth2ClientPage(reqVO).getList()).extracting(OAuth2ClientDO::getClientId)
                .containsExactly(CLIENT_ID);
    }

    // ========== 客户端校验：逐项校验，逐项报错 ==========

    @Test
    void validOAuthClient_全部通过() {
        clientMapper.insert(client());
        OAuth2ClientDO client = clientService.validOAuthClientFromCache(CLIENT_ID, SECRET, "authorization_code",
                List.of("user.read"), REDIRECT_URI);
        assertThat(client.getClientId()).isEqualTo(CLIENT_ID);
    }

    @Test
    void validOAuthClient_各项不通过() {
        clientMapper.insert(client());
        clientMapper.insert(client().setClientId("disabled").setStatus(CommonStatusEnum.DISABLE.getStatus()));

        assertServiceException(() -> clientService.validOAuthClientFromCache("none"), OAUTH2_CLIENT_NOT_EXISTS);
        assertServiceException(() -> clientService.validOAuthClientFromCache("disabled"), OAUTH2_CLIENT_DISABLE);
        assertServiceException(() -> clientService.validOAuthClientFromCache(CLIENT_ID, "wrong", null, null, null),
                OAUTH2_CLIENT_CLIENT_SECRET_ERROR);
        assertServiceException(() -> clientService.validOAuthClientFromCache(CLIENT_ID, null, "implicit", null, null),
                OAUTH2_CLIENT_AUTHORIZED_GRANT_TYPE_NOT_EXISTS);
        assertServiceException(() -> clientService.validOAuthClientFromCache(CLIENT_ID, null, null,
                List.of("user.read", "admin.all"), null), OAUTH2_CLIENT_SCOPE_OVER);
        assertServiceException(() -> clientService.validOAuthClientFromCache(CLIENT_ID, null, null, null,
                "http://evil.com/cb"), OAUTH2_CLIENT_REDIRECT_URI_NOT_MATCH);
    }

    /**
     * 前缀匹配会被 http://localhost:8080/cb.evil.com 或 /cb/../steal 之类的地址绕过，导致授权码被发往攻击者
     */
    @Test
    void validOAuthClient_回调地址必须精确匹配而非前缀匹配() {
        clientMapper.insert(client());
        for (String uri : List.of(REDIRECT_URI + ".evil.com", REDIRECT_URI + "/../steal", REDIRECT_URI + "?x=1")) {
            assertServiceException(() -> clientService.validOAuthClientFromCache(CLIENT_ID, null, null, null, uri),
                    OAUTH2_CLIENT_REDIRECT_URI_NOT_MATCH);
        }
    }

    private static OAuth2ClientSaveReqVO saveReqVO(String clientId) {
        OAuth2ClientSaveReqVO reqVO = new OAuth2ClientSaveReqVO();
        reqVO.setClientId(clientId);
        reqVO.setSecret("secret");
        reqVO.setName("新客户端");
        reqVO.setLogo("https://www.ccloud.local/logo.png");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setAccessTokenValiditySeconds(1800);
        reqVO.setRefreshTokenValiditySeconds(86400);
        reqVO.setRedirectUris(List.of(REDIRECT_URI));
        reqVO.setAuthorizedGrantTypes(List.of("password", "refresh_token"));
        reqVO.setScopes(List.of("user.read"));
        reqVO.setAutoApproveScopes(List.of());
        reqVO.setAuthorities(List.of());
        reqVO.setResourceIds(List.of());
        return reqVO;
    }
}
