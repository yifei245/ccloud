package com.ccloud.module.system.service.social;

import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.api.social.dto.SocialWxPhoneNumberInfoRespDTO;
import com.ccloud.module.system.controller.admin.social.vo.client.SocialClientPageReqVO;
import com.ccloud.module.system.controller.admin.social.vo.client.SocialClientSaveReqVO;
import com.ccloud.module.system.dal.dataobject.social.SocialClientDO;
import com.ccloud.module.system.dal.mysql.social.SocialClientMapper;
import com.ccloud.module.system.enums.social.SocialTypeEnum;
import com.ccloud.module.system.framework.social.SocialClientFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Slf4j
@Service
@Validated
public class SocialClientServiceImpl implements SocialClientService {

    @Resource
    private SocialClientMapper socialClientMapper;
    @Resource
    private SocialClientFactory socialClientFactory;

    @Override
    public Long createSocialClient(SocialClientSaveReqVO createReqVO) {
        if (StrUtil.isEmpty(createReqVO.getClientSecret())) {
            throw invalidParamException("客户端密钥不能为空");
        }
        validateUnique(null, createReqVO.getSocialType(), createReqVO.getUserType());
        SocialClientDO client = BeanUtils.toBean(createReqVO, SocialClientDO.class);
        socialClientMapper.insert(client);
        return client.getId();
    }

    @Override
    public void updateSocialClient(SocialClientSaveReqVO updateReqVO) {
        validateSocialClientExists(updateReqVO.getId());
        validateUnique(updateReqVO.getId(), updateReqVO.getSocialType(), updateReqVO.getUserType());
        SocialClientDO updateObj = BeanUtils.toBean(updateReqVO, SocialClientDO.class);
        if (StrUtil.isEmpty(updateObj.getClientSecret())) {
            updateObj.setClientSecret(null);
        }
        socialClientMapper.updateById(updateObj);
    }

    @Override
    public void deleteSocialClient(Long id) {
        validateSocialClientExists(id);
        socialClientMapper.deleteById(id);
    }

    private void validateSocialClientExists(Long id) {
        if (socialClientMapper.selectById(id) == null) {
            throw exception(SOCIAL_CLIENT_NOT_EXISTS);
        }
    }

    private void validateUnique(Long id, Integer socialType, Integer userType) {
        SocialClientDO client = socialClientMapper.selectBySocialTypeAndUserType(socialType, userType);
        if (client != null && !Objects.equals(client.getId(), id)) {
            throw exception(SOCIAL_CLIENT_UNIQUE);
        }
    }

    @Override
    public SocialClientDO getSocialClient(Long id) {
        return socialClientMapper.selectById(id);
    }

    @Override
    public PageResult<SocialClientDO> getSocialClientPage(SocialClientPageReqVO pageReqVO) {
        return socialClientMapper.selectPage(pageReqVO);
    }

    // ========== 授权 ==========

    @Override
    public String getAuthorizeUrl(Integer socialType, Integer userType, String redirectUri) {
        AuthRequest request = socialClientFactory.buildAuthRequest(getEnabledClient(socialType, userType), redirectUri);
        // authorize 内部会把 state 写入 AuthStateCache（Redis），回调时据此校验
        return request.authorize(AuthStateUtils.createState());
    }

    @Override
    @SuppressWarnings("unchecked")
    public AuthUser getAuthUser(Integer socialType, Integer userType, String code, String state) {
        SocialClientDO client = getEnabledClient(socialType, userType);
        if (SocialTypeEnum.WECHAT_MINI_PROGRAM.getType().equals(socialType)) {
            return getWxMaAuthUser(client, code);
        }
        AuthRequest request = socialClientFactory.buildAuthRequest(client, null);
        AuthResponse<AuthUser> response = request.login(AuthCallback.builder().code(code).state(state).build());
        if (!response.ok()) {
            log.warn("[getAuthUser] 社交平台 {} 授权失败：{} {}", socialType, response.getCode(), response.getMsg());
            throw exception(SOCIAL_USER_AUTH_FAILURE, response.getMsg());
        }
        return response.getData();
    }

    /**
     * 小程序登录不经过网页授权：以 wx.login 得到的 jsCode 换取 openid，没有昵称头像
     */
    private AuthUser getWxMaAuthUser(SocialClientDO client, String jsCode) {
        try {
            WxMaJscode2SessionResult session = socialClientFactory.buildWxMaService(client)
                    .getUserService().getSessionInfo(jsCode);
            JSONObject raw = new JSONObject();
            raw.put("openid", session.getOpenid());
            raw.put("unionid", session.getUnionid());
            return AuthUser.builder().uuid(session.getOpenid()).nickname("")
                    .token(AuthToken.builder().openId(session.getOpenid()).unionId(session.getUnionid()).build())
                    .rawUserInfo(raw).build();
        } catch (WxErrorException e) {
            throw exception(SOCIAL_USER_AUTH_FAILURE, e.getError().getErrorMsg());
        }
    }

    @Override
    public SocialWxPhoneNumberInfoRespDTO getWxMaPhoneNumberInfo(Integer userType, String phoneCode) {
        SocialClientDO client = getEnabledClient(SocialTypeEnum.WECHAT_MINI_PROGRAM.getType(), userType);
        try {
            WxMaPhoneNumberInfo info = socialClientFactory.buildWxMaService(client).getUserService().getPhoneNumber(phoneCode);
            return BeanUtils.toBean(info, SocialWxPhoneNumberInfoRespDTO.class);
        } catch (WxErrorException e) {
            log.warn("[getWxMaPhoneNumberInfo] 换取手机号失败", e);
            throw exception(SOCIAL_CLIENT_WEIXIN_MINI_APP_PHONE_CODE_ERROR);
        }
    }

    private SocialClientDO getEnabledClient(Integer socialType, Integer userType) {
        SocialClientDO client = socialClientMapper.selectBySocialTypeAndUserType(socialType, userType);
        if (client == null || CommonStatusEnum.isDisable(client.getStatus())) {
            throw exception(SOCIAL_CLIENT_NOT_EXISTS);
        }
        return client;
    }
}
