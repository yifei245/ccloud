package com.ccloud.module.system.service.oauth2;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.oauth2.vo.token.OAuth2AccessTokenPageReqVO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;

import java.util.List;

/**
 * OAuth2 令牌 Service：访问令牌存数据库并缓存到 Redis，刷新令牌只存数据库
 */
public interface OAuth2TokenService {

    /**
     * 在当前租户下签发访问令牌与刷新令牌，有效期取自客户端配置
     */
    OAuth2AccessTokenDO createAccessToken(Long userId, Integer userType, String clientId, List<String> scopes);

    /**
     * 作废刷新令牌关联的全部访问令牌，并签发新的访问令牌
     */
    OAuth2AccessTokenDO refreshAccessToken(String refreshToken, String clientId);

    /**
     * 先查 Redis，未命中再查数据库；数据库中未过期时回写 Redis
     *
     * @return 不存在返回 null（不校验是否过期）
     */
    OAuth2AccessTokenDO getAccessToken(String accessToken);

    /**
     * 校验访问令牌有效：不存在或已过期抛出 401
     */
    OAuth2AccessTokenDO checkAccessToken(String accessToken);

    /**
     * 作废访问令牌及其刷新令牌，用于登出
     *
     * @return 被作废的令牌；不存在返回 null
     */
    OAuth2AccessTokenDO removeAccessToken(String accessToken);

    /**
     * 作废用户的全部令牌，用于强制下线、禁用账号、修改密码
     */
    void removeAccessTokensByUser(Long userId, Integer userType);

    /**
     * 分页查询未过期的访问令牌
     */
    PageResult<OAuth2AccessTokenDO> getAccessTokenPage(OAuth2AccessTokenPageReqVO reqVO);
}
