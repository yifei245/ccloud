package com.ccloud.module.system.service.oauth2;

import cn.hutool.core.util.IdUtil;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.system.controller.admin.oauth2.vo.token.OAuth2AccessTokenPageReqVO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import com.ccloud.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import com.ccloud.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import com.ccloud.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants.UNAUTHORIZED;
import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Service
public class OAuth2TokenServiceImpl implements OAuth2TokenService {

    @Resource
    private OAuth2AccessTokenMapper accessTokenMapper;
    @Resource
    private OAuth2RefreshTokenMapper refreshTokenMapper;
    @Resource
    private OAuth2AccessTokenRedisDAO accessTokenRedisDAO;
    @Resource
    private OAuth2ClientService oauth2ClientService;
    @Resource
    @Lazy
    private AdminUserService adminUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuth2AccessTokenDO createAccessToken(Long userId, Integer userType, String clientId, List<String> scopes) {
        OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(clientId);
        OAuth2RefreshTokenDO refreshToken = createRefreshToken(userId, userType, client, scopes);
        return createAccessToken(refreshToken, userId, userType, client, scopes);
    }

    /**
     * @return 客户端未配置刷新令牌有效期时返回 null
     */
    private OAuth2RefreshTokenDO createRefreshToken(Long userId, Integer userType, OAuth2ClientDO client,
                                                    List<String> scopes) {
        if (client.getRefreshTokenValiditySeconds() == null || client.getRefreshTokenValiditySeconds() <= 0) {
            return null;
        }
        OAuth2RefreshTokenDO refreshToken = new OAuth2RefreshTokenDO().setRefreshToken(generateToken())
                .setUserId(userId).setUserType(userType).setClientId(client.getClientId()).setScopes(scopes)
                .setExpiresTime(LocalDateTime.now().plusSeconds(client.getRefreshTokenValiditySeconds()));
        refreshTokenMapper.insert(refreshToken);
        return refreshToken;
    }

    private OAuth2AccessTokenDO createAccessToken(OAuth2RefreshTokenDO refreshToken, Long userId, Integer userType,
                                                  OAuth2ClientDO client, List<String> scopes) {
        OAuth2AccessTokenDO accessToken = new OAuth2AccessTokenDO().setAccessToken(generateToken())
                .setRefreshToken(refreshToken != null ? refreshToken.getRefreshToken() : "")
                .setUserId(userId).setUserType(userType).setUserInfo(buildUserInfo(userId, userType))
                .setClientId(client.getClientId()).setScopes(scopes)
                .setExpiresTime(LocalDateTime.now().plusSeconds(client.getAccessTokenValiditySeconds()));
        accessTokenMapper.insert(accessToken);
        accessTokenRedisDAO.set(accessToken);
        return accessToken;
    }

    /**
     * 用户信息快照随令牌下发：网关写入内部请求头，各服务无需回查即可得到昵称、部门等。
     * 会员的快照由会员服务接入时补充
     */
    private Map<String, String> buildUserInfo(Long userId, Integer userType) {
        Map<String, String> userInfo = new HashMap<>();
        if (UserTypeEnum.ADMIN.getValue().equals(userType) && userId != null && userId > 0) {
            AdminUserDO user = adminUserService.getUser(userId);
            if (user != null) {
                userInfo.put("nickname", user.getNickname());
                if (user.getDeptId() != null) {
                    userInfo.put("deptId", user.getDeptId().toString());
                }
            }
        }
        return userInfo;
    }

    /**
     * 32 位随机令牌，基于安全随机数
     */
    private static String generateToken() {
        return IdUtil.fastSimpleUUID();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = com.ccloud.framework.common.exception.ServiceException.class)
    public OAuth2AccessTokenDO refreshAccessToken(String refreshToken, String clientId) {
        OAuth2RefreshTokenDO refreshTokenDO = refreshTokenMapper.selectByRefreshToken(refreshToken);
        if (refreshTokenDO == null) {
            throw exception(OAUTH2_REFRESH_TOKEN_NOT_EXISTS);
        }
        OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(clientId);
        if (!Objects.equals(clientId, refreshTokenDO.getClientId())) {
            throw exception(OAUTH2_REFRESH_TOKEN_CLIENT_MISMATCH);
        }

        // 作废该刷新令牌已签发的全部访问令牌
        removeAccessTokens(accessTokenMapper.selectListByRefreshTokenIgnoreTenant(refreshToken));

        if (refreshTokenDO.getExpiresTime().isBefore(LocalDateTime.now())) {
            refreshTokenMapper.deleteByRefreshToken(refreshToken);
            throw exception(OAUTH2_REFRESH_TOKEN_EXPIRED);
        }
        // 新令牌归属刷新令牌所在租户，而不是当前请求的租户
        return TenantUtils.execute(refreshTokenDO.getTenantId(), () -> createAccessToken(refreshTokenDO,
                refreshTokenDO.getUserId(), refreshTokenDO.getUserType(), client, refreshTokenDO.getScopes()));
    }

    @Override
    public OAuth2AccessTokenDO getAccessToken(String accessToken) {
        OAuth2AccessTokenDO token = accessTokenRedisDAO.get(accessToken);
        if (token != null) {
            return token;
        }
        token = accessTokenMapper.selectByAccessTokenIgnoreTenant(accessToken);
        if (token != null && token.getExpiresTime().isAfter(LocalDateTime.now())) {
            accessTokenRedisDAO.set(token);
        }
        return token;
    }

    @Override
    public OAuth2AccessTokenDO checkAccessToken(String accessToken) {
        OAuth2AccessTokenDO token = getAccessToken(accessToken);
        if (token == null) {
            throw exception(UNAUTHORIZED.getCode(), "访问令牌不存在");
        }
        if (token.getExpiresTime().isBefore(LocalDateTime.now())) {
            removeAccessTokens(Collections.singletonList(token));
            throw exception(UNAUTHORIZED.getCode(), "访问令牌已过期");
        }
        return token;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuth2AccessTokenDO removeAccessToken(String accessToken) {
        OAuth2AccessTokenDO token = accessTokenMapper.selectByAccessTokenIgnoreTenant(accessToken);
        if (token == null) {
            return null;
        }
        removeAccessTokens(Collections.singletonList(token));
        if (!token.getRefreshToken().isEmpty()) {
            refreshTokenMapper.deleteByRefreshToken(token.getRefreshToken());
        }
        return token;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAccessTokensByUser(Long userId, Integer userType) {
        List<OAuth2AccessTokenDO> tokens = accessTokenMapper.selectListByUserIdAndUserType(userId, userType);
        removeAccessTokens(tokens);
        tokens.stream().map(OAuth2AccessTokenDO::getRefreshToken).filter(token -> !token.isEmpty()).distinct()
                .forEach(refreshTokenMapper::deleteByRefreshToken);
    }

    private void removeAccessTokens(List<OAuth2AccessTokenDO> tokens) {
        if (tokens.isEmpty()) {
            return;
        }
        tokens.forEach(token -> accessTokenMapper.deleteByIdIgnoreTenant(token.getId()));
        accessTokenRedisDAO.deleteList(CollectionUtils.convertList(tokens, OAuth2AccessTokenDO::getAccessToken));
    }

    @Override
    public PageResult<OAuth2AccessTokenDO> getAccessTokenPage(OAuth2AccessTokenPageReqVO reqVO) {
        return accessTokenMapper.selectPage(reqVO);
    }
}
