package com.ccloud.module.system.api.oauth2;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.api.oauth2.dto.OAuth2AccessTokenCheckRespDTO;
import com.ccloud.module.system.api.oauth2.dto.OAuth2AccessTokenCreateReqDTO;
import com.ccloud.module.system.api.oauth2.dto.OAuth2AccessTokenRespDTO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import com.ccloud.module.system.service.oauth2.OAuth2TokenService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * 令牌 RPC 实现：网关校验令牌、会员服务登录签发令牌依赖它
 */
@RestController
@Validated
public class OAuth2TokenApiImpl implements OAuth2TokenApi {

    @Resource
    private OAuth2TokenService oauth2TokenService;

    @Override
    public CommonResult<OAuth2AccessTokenRespDTO> createAccessToken(OAuth2AccessTokenCreateReqDTO reqDTO) {
        OAuth2AccessTokenDO accessToken = oauth2TokenService.createAccessToken(
                reqDTO.getUserId(), reqDTO.getUserType(), reqDTO.getClientId(), reqDTO.getScopes());
        return success(BeanUtils.toBean(accessToken, OAuth2AccessTokenRespDTO.class));
    }

    @Override
    public CommonResult<OAuth2AccessTokenCheckRespDTO> checkAccessToken(String accessToken) {
        return success(BeanUtils.toBean(oauth2TokenService.checkAccessToken(accessToken),
                OAuth2AccessTokenCheckRespDTO.class));
    }

    @Override
    public CommonResult<OAuth2AccessTokenRespDTO> removeAccessToken(String accessToken) {
        return success(BeanUtils.toBean(oauth2TokenService.removeAccessToken(accessToken),
                OAuth2AccessTokenRespDTO.class));
    }

    @Override
    public CommonResult<Boolean> removeAccessTokensByUser(Long userId, Integer userType) {
        oauth2TokenService.removeAccessTokensByUser(userId, userType);
        return success(true);
    }

    @Override
    public CommonResult<OAuth2AccessTokenRespDTO> refreshAccessToken(String refreshToken, String clientId) {
        return success(BeanUtils.toBean(oauth2TokenService.refreshAccessToken(refreshToken, clientId),
                OAuth2AccessTokenRespDTO.class));
    }
}
