package com.ccloud.module.system.service.social;

import com.ccloud.module.system.api.social.dto.SocialUserBindReqDTO;
import com.ccloud.module.system.api.social.dto.SocialUserRespDTO;
import com.ccloud.module.system.api.social.dto.SocialUserUnbindReqDTO;
import com.ccloud.module.system.dal.dataobject.social.SocialUserDO;
import jakarta.validation.Valid;

/**
 * 社交用户 Service：社交平台账号与本系统用户的绑定关系
 */
public interface SocialUserService {

    /**
     * 用授权码换取社交用户并落库。同一授权码与 state 已换取过时直接返回本地记录（授权码只能用一次）
     */
    SocialUserDO authSocialUser(Integer socialType, Integer userType, String code, String state);

    /**
     * 绑定社交账号：该账号已绑定其他用户时失败；同一用户换绑同平台另一账号时替换旧绑定
     *
     * @return 社交平台 openid
     */
    String bindSocialUser(@Valid SocialUserBindReqDTO reqDTO);

    void unbindSocialUser(@Valid SocialUserUnbindReqDTO reqDTO);

    /**
     * @return 用户在该平台绑定的社交账号；未绑定返回 null
     */
    SocialUserRespDTO getSocialUserByUserId(Integer userType, Long userId, Integer socialType);

    /**
     * 用授权码获得社交用户及其绑定的用户编号，未绑定时 userId 为 null。社交登录据此判断是否已绑定
     */
    SocialUserRespDTO getSocialUserByCode(Integer userType, Integer socialType, String code, String state);
}
