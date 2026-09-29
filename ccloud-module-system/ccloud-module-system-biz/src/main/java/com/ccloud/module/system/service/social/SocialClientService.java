package com.ccloud.module.system.service.social;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.api.social.dto.SocialWxPhoneNumberInfoRespDTO;
import com.ccloud.module.system.controller.admin.social.vo.client.SocialClientPageReqVO;
import com.ccloud.module.system.controller.admin.social.vo.client.SocialClientSaveReqVO;
import com.ccloud.module.system.dal.dataobject.social.SocialClientDO;
import jakarta.validation.Valid;
import me.zhyd.oauth.model.AuthUser;

/**
 * 社交客户端 Service：管理各租户的社交应用配置，并代理与社交平台的授权交互
 */
public interface SocialClientService {

    /**
     * 同一租户、平台、用户类型只允许一个；密钥必填
     */
    Long createSocialClient(@Valid SocialClientSaveReqVO createReqVO);

    /**
     * 密钥为空时保留原密钥：管理页面不回显密钥
     */
    void updateSocialClient(@Valid SocialClientSaveReqVO updateReqVO);

    void deleteSocialClient(Long id);

    SocialClientDO getSocialClient(Long id);

    PageResult<SocialClientDO> getSocialClientPage(SocialClientPageReqVO pageReqVO);

    /**
     * 获得社交平台授权页地址，并把 state 存入 Redis 供回调校验
     */
    String getAuthorizeUrl(Integer socialType, Integer userType, String redirectUri);

    /**
     * 用授权码换取社交平台用户；微信小程序以 jsCode 换取 openid
     */
    AuthUser getAuthUser(Integer socialType, Integer userType, String code, String state);

    /**
     * 微信小程序以 phoneCode 换取手机号
     */
    SocialWxPhoneNumberInfoRespDTO getWxMaPhoneNumberInfo(Integer userType, String phoneCode);
}
