package com.ccloud.module.system.service.oauth2;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.module.system.controller.admin.oauth2.vo.token.OAuth2AccessTokenPageReqVO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import com.ccloud.module.system.dal.mysql.oauth2.OAuth2ClientMapper;
import com.ccloud.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import com.ccloud.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import com.ccloud.module.system.service.user.AdminUserService;
import com.ccloud.module.system.test.BaseSystemDbAndRedisUnitTest;
import com.ccloud.module.system.test.CacheTestConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants.UNAUTHORIZED;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static com.ccloud.module.system.service.oauth2.OAuth2TestData.CLIENT_ID;
import static com.ccloud.module.system.service.oauth2.OAuth2TestData.client;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@Import({OAuth2TokenServiceImpl.class, OAuth2ClientServiceImpl.class, OAuth2AccessTokenRedisDAO.class,
        CacheTestConfiguration.class})
class OAuth2TokenServiceImplTest extends BaseSystemDbAndRedisUnitTest {

    @Resource
    private OAuth2TokenService tokenService;
    @Resource
    private OAuth2AccessTokenMapper accessTokenMapper;
    @Resource
    private OAuth2RefreshTokenMapper refreshTokenMapper;
    @Resource
    private OAuth2ClientMapper clientMapper;
    @Resource
    private OAuth2AccessTokenRedisDAO redisDAO;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private AdminUserService userService;

    @BeforeEach
    void seed() {
        clientMapper.insert(client());
        when(userService.getUser(1L)).thenReturn(new AdminUserDO().setId(1L).setNickname("管理员").setDeptId(100L));
    }

    @Test
    void createAccessToken应同时写DB与Redis且过期时间取自客户端配置() {
        OAuth2AccessTokenDO token = tokenService.createAccessToken(1L, UserTypeEnum.ADMIN.getValue(), CLIENT_ID,
                List.of("user.read"));

        OAuth2AccessTokenDO db = selectAccessToken(token.getAccessToken());
        assertThat(db.getUserId()).isEqualTo(1L);
        assertThat(db.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(db.getScopes()).containsExactly("user.read");
        assertThat(db.getUserInfo()).containsEntry("nickname", "管理员").containsEntry("deptId", "100");
        assertThat(Duration.between(LocalDateTime.now(), db.getExpiresTime()).getSeconds()).isBetween(1790L, 1800L);

        OAuth2RefreshTokenDO refresh = refreshTokenMapper.selectByRefreshToken(token.getRefreshToken());
        assertThat(Duration.between(LocalDateTime.now(), refresh.getExpiresTime()).toHours()).isBetween(23L, 24L);

        assertThat(redisDAO.get(token.getAccessToken()).getUserId()).isEqualTo(1L);
        Long ttl = stringRedisTemplate.getExpire(OAuth2AccessTokenRedisDAO.formatKey(token.getAccessToken()));
        assertThat(ttl).isBetween(1790L, 1800L);
    }

    @Test
    void getAccessToken_Redis命中时不查DB() {
        OAuth2AccessTokenDO token = createToken();
        // 删掉数据库中的记录：若仍能取到，说明走的是 Redis
        accessTokenMapper.deleteById(selectAccessToken(token.getAccessToken()).getId());
        assertThat(tokenService.getAccessToken(token.getAccessToken())).isNotNull();
    }

    @Test
    void getAccessToken_Redis未命中但DB有效时应回写Redis() {
        OAuth2AccessTokenDO token = createToken();
        redisDAO.delete(token.getAccessToken());

        assertThat(tokenService.getAccessToken(token.getAccessToken())).isNotNull();

        assertThat(redisDAO.get(token.getAccessToken())).isNotNull();
    }

    @Test
    void getAccessToken_跨租户也能查到_网关校验时不知道租户() {
        OAuth2AccessTokenDO token = createToken();
        redisDAO.delete(token.getAccessToken());
        TenantContextHolder.setTenantId(999L);
        assertThat(tokenService.getAccessToken(token.getAccessToken()).getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void checkAccessToken_有效时返回() {
        OAuth2AccessTokenDO token = createToken();
        assertThat(tokenService.checkAccessToken(token.getAccessToken()).getUserId()).isEqualTo(1L);
    }

    @Test
    void checkAccessToken_令牌不存在应抛UNAUTHORIZED() {
        assertServiceException(() -> tokenService.checkAccessToken("not-exists"), UNAUTHORIZED);
    }

    @Test
    void checkAccessToken_令牌已过期应抛UNAUTHORIZED并清理() {
        OAuth2AccessTokenDO token = createToken();
        redisDAO.delete(token.getAccessToken());
        OAuth2AccessTokenDO db = selectAccessToken(token.getAccessToken());
        accessTokenMapper.updateById(new OAuth2AccessTokenDO().setId(db.getId())
                .setExpiresTime(LocalDateTime.now().minusSeconds(1)));

        assertThatThrownBy(() -> tokenService.checkAccessToken(token.getAccessToken()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("过期")
                .extracting("code").isEqualTo(UNAUTHORIZED.getCode());
        assertThat(selectAccessToken(token.getAccessToken())).isNull();
    }

    @Test
    void refreshAccessToken_应作废旧accessToken并签发新的() {
        OAuth2AccessTokenDO old = createToken();

        OAuth2AccessTokenDO fresh = tokenService.refreshAccessToken(old.getRefreshToken(), CLIENT_ID);

        assertThat(fresh.getAccessToken()).isNotEqualTo(old.getAccessToken());
        assertThat(fresh.getRefreshToken()).isEqualTo(old.getRefreshToken());
        assertThat(selectAccessToken(old.getAccessToken())).isNull();
        assertThat(redisDAO.get(old.getAccessToken())).isNull();
        assertThat(tokenService.checkAccessToken(fresh.getAccessToken()).getUserId()).isEqualTo(1L);
    }

    @Test
    void refreshAccessToken_refreshToken不存在() {
        assertServiceException(() -> tokenService.refreshAccessToken("none", CLIENT_ID), OAUTH2_REFRESH_TOKEN_NOT_EXISTS);
    }

    @Test
    void refreshAccessToken_refreshToken过期应抛异常并删除() {
        OAuth2AccessTokenDO token = createToken();
        OAuth2RefreshTokenDO refresh = refreshTokenMapper.selectByRefreshToken(token.getRefreshToken());
        refreshTokenMapper.updateById(new OAuth2RefreshTokenDO().setId(refresh.getId())
                .setExpiresTime(LocalDateTime.now().minusSeconds(1)));

        assertServiceException(() -> tokenService.refreshAccessToken(token.getRefreshToken(), CLIENT_ID),
                OAUTH2_REFRESH_TOKEN_EXPIRED);
        assertThat(refreshTokenMapper.selectByRefreshToken(token.getRefreshToken())).isNull();
    }

    @Test
    void refreshAccessToken_clientId不匹配应抛异常且不作废令牌() {
        clientMapper.insert(client().setClientId("other-client"));
        OAuth2AccessTokenDO token = createToken();
        assertServiceException(() -> tokenService.refreshAccessToken(token.getRefreshToken(), "other-client"),
                OAUTH2_REFRESH_TOKEN_CLIENT_MISMATCH);
        assertThat(tokenService.checkAccessToken(token.getAccessToken())).isNotNull();
    }

    @Test
    void removeAccessToken应同时清DB与Redis以及刷新令牌() {
        OAuth2AccessTokenDO token = createToken();

        OAuth2AccessTokenDO removed = tokenService.removeAccessToken(token.getAccessToken());

        assertThat(removed.getAccessToken()).isEqualTo(token.getAccessToken());
        assertThat(selectAccessToken(token.getAccessToken())).isNull();
        assertThat(redisDAO.get(token.getAccessToken())).isNull();
        assertThat(refreshTokenMapper.selectByRefreshToken(token.getRefreshToken())).isNull();
        assertThat(tokenService.removeAccessToken("none")).isNull();
    }

    @Test
    void createAccessToken_客户端未配置刷新令牌有效期时不签发刷新令牌() {
        clientMapper.insert(client().setClientId("no-refresh").setRefreshTokenValiditySeconds(0));
        OAuth2AccessTokenDO token = tokenService.createAccessToken(0L, UserTypeEnum.ADMIN.getValue(), "no-refresh", List.of());
        assertThat(token.getRefreshToken()).isEmpty();
        assertThat(tokenService.checkAccessToken(token.getAccessToken())).isNotNull();
    }

    @Test
    void getAccessTokenPage_只返回未过期的() {
        OAuth2AccessTokenDO valid = createToken();
        OAuth2AccessTokenDO expired = createToken();
        OAuth2AccessTokenDO db = selectAccessToken(expired.getAccessToken());
        accessTokenMapper.updateById(new OAuth2AccessTokenDO().setId(db.getId()).setExpiresTime(LocalDateTime.now().minusSeconds(1)));

        OAuth2AccessTokenPageReqVO reqVO = new OAuth2AccessTokenPageReqVO();
        reqVO.setUserId(1L);
        assertThat(tokenService.getAccessTokenPage(reqVO).getList()).extracting(OAuth2AccessTokenDO::getAccessToken)
                .containsExactly(valid.getAccessToken());
    }

    @Test
    void removeAccessTokensByUser_作废用户的全部令牌() {
        OAuth2AccessTokenDO a = createToken();
        OAuth2AccessTokenDO b = createToken();
        tokenService.removeAccessTokensByUser(1L, UserTypeEnum.ADMIN.getValue());
        assertServiceException(() -> tokenService.checkAccessToken(a.getAccessToken()), UNAUTHORIZED);
        assertServiceException(() -> tokenService.checkAccessToken(b.getAccessToken()), UNAUTHORIZED);
    }

    @Test
    void removeAccessTokensByUser_访问令牌已过期被清理后_其刷新令牌也应作废() {
        OAuth2AccessTokenDO token = createToken();
        // 访问令牌过期后在校验时被删除，只剩刷新令牌
        redisDAO.delete(token.getAccessToken());
        OAuth2AccessTokenDO db = selectAccessToken(token.getAccessToken());
        accessTokenMapper.updateById(new OAuth2AccessTokenDO().setId(db.getId())
                .setExpiresTime(LocalDateTime.now().minusSeconds(1)));
        assertServiceException(() -> tokenService.checkAccessToken(token.getAccessToken()), UNAUTHORIZED);

        tokenService.removeAccessTokensByUser(1L, UserTypeEnum.ADMIN.getValue());

        assertServiceException(() -> tokenService.refreshAccessToken(token.getRefreshToken(), CLIENT_ID),
                OAUTH2_REFRESH_TOKEN_NOT_EXISTS);
    }

    @Test
    void removeAccessTokensByUser_在其他租户上下文中调用也应作废() {
        OAuth2AccessTokenDO token = createToken();
        TenantContextHolder.setTenantId(999L);

        tokenService.removeAccessTokensByUser(1L, UserTypeEnum.ADMIN.getValue());

        assertServiceException(() -> tokenService.checkAccessToken(token.getAccessToken()), UNAUTHORIZED);
        assertServiceException(() -> tokenService.refreshAccessToken(token.getRefreshToken(), CLIENT_ID),
                OAUTH2_REFRESH_TOKEN_NOT_EXISTS);
    }

    @Test
    void removeAccessTokensByUser_不影响其他用户与同编号的其他用户类型() {
        OAuth2AccessTokenDO otherUser = tokenService.createAccessToken(2L, UserTypeEnum.ADMIN.getValue(), CLIENT_ID, List.of());
        OAuth2AccessTokenDO member = tokenService.createAccessToken(1L, UserTypeEnum.MEMBER.getValue(), CLIENT_ID, List.of());

        tokenService.removeAccessTokensByUser(1L, UserTypeEnum.ADMIN.getValue());

        assertThat(tokenService.checkAccessToken(otherUser.getAccessToken())).isNotNull();
        assertThat(tokenService.checkAccessToken(member.getAccessToken())).isNotNull();
        assertThat(tokenService.refreshAccessToken(otherUser.getRefreshToken(), CLIENT_ID)).isNotNull();
        assertThat(tokenService.refreshAccessToken(member.getRefreshToken(), CLIENT_ID)).isNotNull();
    }

    private OAuth2AccessTokenDO createToken() {
        return tokenService.createAccessToken(1L, UserTypeEnum.ADMIN.getValue(), CLIENT_ID, List.of("user.read"));
    }

    private OAuth2AccessTokenDO selectAccessToken(String accessToken) {
        return accessTokenMapper.selectByAccessTokenIgnoreTenant(accessToken);
    }
}
