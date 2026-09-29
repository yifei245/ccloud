package com.ccloud.module.system.service.oauth2;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.http.HttpUtils;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.oauth2.vo.client.OAuth2ClientPageReqVO;
import com.ccloud.module.system.controller.admin.oauth2.vo.client.OAuth2ClientSaveReqVO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import com.ccloud.module.system.dal.mysql.oauth2.OAuth2ClientMapper;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collection;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.dal.redis.RedisKeyConstants.OAUTH2_CLIENT;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class OAuth2ClientServiceImpl implements OAuth2ClientService {

    @Resource
    private OAuth2ClientMapper oauth2ClientMapper;
    /**
     * 自身代理：带缓存注解的方法在类内调用时必须经过代理才生效
     */
    @Resource
    @Lazy
    private OAuth2ClientService self;

    @Override
    public Long createOAuth2Client(OAuth2ClientSaveReqVO createReqVO) {
        validateClientIdUnique(null, createReqVO.getClientId());
        OAuth2ClientDO client = BeanUtils.toBean(createReqVO, OAuth2ClientDO.class);
        oauth2ClientMapper.insert(client);
        return client.getId();
    }

    @Override
    @CacheEvict(value = OAUTH2_CLIENT, allEntries = true)
    public void updateOAuth2Client(OAuth2ClientSaveReqVO updateReqVO) {
        validateOAuth2ClientExists(updateReqVO.getId());
        validateClientIdUnique(updateReqVO.getId(), updateReqVO.getClientId());
        oauth2ClientMapper.updateById(BeanUtils.toBean(updateReqVO, OAuth2ClientDO.class));
    }

    @Override
    @CacheEvict(value = OAUTH2_CLIENT, allEntries = true)
    public void deleteOAuth2Client(Long id) {
        validateOAuth2ClientExists(id);
        oauth2ClientMapper.deleteById(id);
    }

    private void validateOAuth2ClientExists(Long id) {
        if (id == null || oauth2ClientMapper.selectById(id) == null) {
            throw exception(OAUTH2_CLIENT_NOT_EXISTS);
        }
    }

    private void validateClientIdUnique(Long id, String clientId) {
        OAuth2ClientDO client = oauth2ClientMapper.selectByClientId(clientId);
        if (client != null && !client.getId().equals(id)) {
            throw exception(OAUTH2_CLIENT_EXISTS);
        }
    }

    @Override
    public OAuth2ClientDO getOAuth2Client(Long id) {
        return oauth2ClientMapper.selectById(id);
    }

    @Override
    @Cacheable(value = OAUTH2_CLIENT, key = "#clientId", unless = "#result == null")
    public OAuth2ClientDO getOAuth2ClientFromCache(String clientId) {
        return oauth2ClientMapper.selectByClientId(clientId);
    }

    @Override
    public PageResult<OAuth2ClientDO> getOAuth2ClientPage(OAuth2ClientPageReqVO pageReqVO) {
        return oauth2ClientMapper.selectPage(pageReqVO);
    }

    @Override
    public OAuth2ClientDO validOAuthClientFromCache(String clientId, String clientSecret, String authorizedGrantType,
                                                    Collection<String> scopes, String redirectUri) {
        OAuth2ClientDO client = self.getOAuth2ClientFromCache(clientId);
        if (client == null) {
            throw exception(OAUTH2_CLIENT_NOT_EXISTS);
        }
        if (!CommonStatusEnum.isEnable(client.getStatus())) {
            throw exception(OAUTH2_CLIENT_DISABLE);
        }
        if (StrUtil.isNotEmpty(clientSecret) && !constantTimeEquals(clientSecret, client.getSecret())) {
            throw exception(OAUTH2_CLIENT_CLIENT_SECRET_ERROR, clientSecret);
        }
        if (StrUtil.isNotEmpty(authorizedGrantType)
                && !CollUtil.contains(client.getAuthorizedGrantTypes(), authorizedGrantType)) {
            throw exception(OAUTH2_CLIENT_AUTHORIZED_GRANT_TYPE_NOT_EXISTS);
        }
        if (CollUtil.isNotEmpty(scopes) && !CollUtil.containsAll(client.getScopes(), scopes)) {
            throw exception(OAUTH2_CLIENT_SCOPE_OVER);
        }
        if (StrUtil.isNotEmpty(redirectUri) && !HttpUtils.isRedirectUriValid(redirectUri, client.getRedirectUris())) {
            throw exception(OAUTH2_CLIENT_REDIRECT_URI_NOT_MATCH, redirectUri);
        }
        return client;
    }

    /**
     * 常量时间比较，避免按响应耗时逐字符猜出密钥
     */
    private static boolean constantTimeEquals(String a, String b) {
        return b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
