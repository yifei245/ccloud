package com.ccloud.module.system.service.oauth2;

import cn.hutool.core.util.IdUtil;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2CodeDO;
import com.ccloud.module.system.dal.mysql.oauth2.OAuth2CodeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.OAUTH2_CODE_EXPIRE;
import static com.ccloud.module.system.enums.ErrorCodeConstants.OAUTH2_CODE_NOT_EXISTS;

@Service
public class OAuth2CodeServiceImpl implements OAuth2CodeService {

    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    @Resource
    private OAuth2CodeMapper oauth2CodeMapper;

    @Override
    public OAuth2CodeDO createAuthorizationCode(Long userId, Integer userType, String clientId, List<String> scopes,
                                                String redirectUri, String state) {
        OAuth2CodeDO code = new OAuth2CodeDO().setCode(IdUtil.fastSimpleUUID())
                .setUserId(userId).setUserType(userType).setClientId(clientId).setScopes(scopes)
                .setExpiresTime(LocalDateTime.now().plus(TIMEOUT))
                .setRedirectUri(redirectUri).setState(state == null ? "" : state);
        oauth2CodeMapper.insert(code);
        return code;
    }

    @Override
    public OAuth2CodeDO getAuthorizationCode(String code) {
        return oauth2CodeMapper.selectByCode(code);
    }

    @Override
    public OAuth2CodeDO consumeAuthorizationCode(String code) {
        OAuth2CodeDO codeDO = oauth2CodeMapper.selectByCode(code);
        // 以作废成功的行数判定：两个请求并发消费同一授权码时，只有一个能成功
        if (codeDO == null || oauth2CodeMapper.deleteByIdIgnoreTenant(codeDO.getId()) == 0) {
            throw exception(OAUTH2_CODE_NOT_EXISTS);
        }
        if (codeDO.getExpiresTime().isBefore(LocalDateTime.now())) {
            throw exception(OAUTH2_CODE_EXPIRE);
        }
        return codeDO;
    }
}
