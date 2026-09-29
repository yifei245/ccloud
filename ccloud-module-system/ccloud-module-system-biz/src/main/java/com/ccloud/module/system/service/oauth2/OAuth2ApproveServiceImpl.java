package com.ccloud.module.system.service.oauth2;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2ApproveDO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import com.ccloud.module.system.dal.mysql.oauth2.OAuth2ApproveMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OAuth2ApproveServiceImpl implements OAuth2ApproveService {

    /**
     * 同意记录的有效期
     */
    private static final Duration TIMEOUT = Duration.ofDays(30);

    @Resource
    private OAuth2ApproveMapper oauth2ApproveMapper;
    @Resource
    private OAuth2ClientService oauth2ClientService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean checkForPreApproval(Long userId, Integer userType, String clientId, Collection<String> requestedScopes) {
        OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(clientId);
        if (CollUtil.containsAll(client.getAutoApproveScopes(), requestedScopes)) {
            requestedScopes.forEach(scope -> saveApprove(userId, userType, clientId, scope, true));
            return true;
        }
        Set<String> approvedScopes = CollectionUtils.convertSet(CollectionUtils.filterList(
                getApproveList(userId, userType, clientId), OAuth2ApproveDO::getApproved), OAuth2ApproveDO::getScope);
        return approvedScopes.containsAll(requestedScopes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAfterApproval(Long userId, Integer userType, String clientId, Map<String, Boolean> requestedScopes) {
        if (CollUtil.isEmpty(requestedScopes)) {
            return true;
        }
        boolean anyApproved = false;
        for (Map.Entry<String, Boolean> entry : requestedScopes.entrySet()) {
            boolean approved = Boolean.TRUE.equals(entry.getValue());
            saveApprove(userId, userType, clientId, entry.getKey(), approved);
            anyApproved |= approved;
        }
        return anyApproved;
    }

    @Override
    public List<OAuth2ApproveDO> getApproveList(Long userId, Integer userType, String clientId) {
        LocalDateTime now = LocalDateTime.now();
        return CollectionUtils.filterList(
                oauth2ApproveMapper.selectListByUserIdAndUserTypeAndClientId(userId, userType, clientId),
                approve -> approve.getExpiresTime().isAfter(now));
    }

    @Override
    public Set<String> getApprovedClientIds(Long userId, Integer userType) {
        LocalDateTime now = LocalDateTime.now();
        return CollectionUtils.convertSet(CollectionUtils.filterList(
                oauth2ApproveMapper.selectListByUserIdAndUserType(userId, userType),
                approve -> Boolean.TRUE.equals(approve.getApproved()) && approve.getExpiresTime().isAfter(now)), OAuth2ApproveDO::getClientId);
    }

    /**
     * 每个用户、客户端、范围只保留一条记录：存在则更新
     */
    private void saveApprove(Long userId, Integer userType, String clientId, String scope, boolean approved) {
        LocalDateTime expiresTime = LocalDateTime.now().plus(TIMEOUT);
        OAuth2ApproveDO existing = oauth2ApproveMapper.selectOne(userId, userType, clientId, scope);
        if (existing != null) {
            oauth2ApproveMapper.updateById(new OAuth2ApproveDO().setId(existing.getId())
                    .setApproved(approved).setExpiresTime(expiresTime));
            return;
        }
        oauth2ApproveMapper.insert(new OAuth2ApproveDO().setUserId(userId).setUserType(userType)
                .setClientId(clientId).setScope(scope).setApproved(approved).setExpiresTime(expiresTime));
    }
}
