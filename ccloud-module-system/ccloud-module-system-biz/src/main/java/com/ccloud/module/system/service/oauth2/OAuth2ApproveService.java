package com.ccloud.module.system.service.oauth2;

import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2ApproveDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OAuth2 授权同意 Service
 */
public interface OAuth2ApproveService {

    /**
     * 申请的范围是否无需用户确认：每个范围要么在客户端的自动授权范围内，要么用户此前已同意且未过期。
     * 通过时记录自动授权的范围
     */
    boolean checkForPreApproval(Long userId, Integer userType, String clientId, Collection<String> requestedScopes);

    /**
     * 保存用户的同意/拒绝结果
     *
     * @param requestedScopes 范围 → 是否同意
     * @return 任一范围同意（或未申请任何范围）时返回 true
     */
    boolean updateAfterApproval(Long userId, Integer userType, String clientId, Map<String, Boolean> requestedScopes);

    /**
     * @return 未过期的同意记录
     */
    List<OAuth2ApproveDO> getApproveList(Long userId, Integer userType, String clientId);

    /**
     * 用户同意过（未过期）的客户端，用于统一登出时告知前端需要一并登出的应用
     */
    Set<String> getApprovedClientIds(Long userId, Integer userType);
}
