package com.ccloud.module.system.service.social;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.module.system.api.social.dto.SocialUserBindReqDTO;
import com.ccloud.module.system.api.social.dto.SocialUserRespDTO;
import com.ccloud.module.system.api.social.dto.SocialUserUnbindReqDTO;
import com.ccloud.module.system.dal.dataobject.social.SocialUserBindDO;
import com.ccloud.module.system.dal.dataobject.social.SocialUserDO;
import com.ccloud.module.system.dal.mysql.social.SocialUserBindMapper;
import com.ccloud.module.system.dal.mysql.social.SocialUserMapper;
import jakarta.annotation.Resource;
import me.zhyd.oauth.model.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.SOCIAL_USER_BIND_DUPLICATE;

@Service
@Validated
public class SocialUserServiceImpl implements SocialUserService {

    /**
     * 同一授权码可复用的时长，小于各平台授权码自身的有效期（约 5 分钟）
     */
    static final Duration CODE_REUSE_WINDOW = Duration.ofMinutes(3);

    @Resource
    private SocialUserMapper socialUserMapper;
    @Resource
    private SocialUserBindMapper socialUserBindMapper;
    @Resource
    private SocialClientService socialClientService;

    @Override
    public SocialUserDO authSocialUser(Integer socialType, Integer userType, String code, String state) {
        // 授权码只能用一次：同一次授权内的后续步骤（登录失败后绑定）复用本地记录。
        // 复用限定在短时间内，否则泄露的旧授权码（日志、Referer）可被无限期重放冒充登录
        SocialUserDO existing = socialUserMapper.selectByTypeAndCodeAndState(socialType, code, state);
        if (existing != null && existing.getUpdateTime().isAfter(LocalDateTime.now().minus(CODE_REUSE_WINDOW))) {
            return existing;
        }
        AuthUser authUser = socialClientService.getAuthUser(socialType, userType, code, state);
        SocialUserDO socialUser = socialUserMapper.selectByTypeAndOpenid(socialType, authUser.getUuid());
        SocialUserDO data = new SocialUserDO().setType(socialType).setOpenid(authUser.getUuid())
                .setToken(authUser.getToken() != null ? authUser.getToken().getAccessToken() : null)
                .setRawTokenInfo(StrUtil.nullToEmpty(JsonUtils.toJsonString(authUser.getToken())))
                .setNickname(StrUtil.sub(StrUtil.nullToEmpty(authUser.getNickname()), 0, 32))
                .setAvatar(authUser.getAvatar())
                .setRawUserInfo(StrUtil.sub(authUser.getRawUserInfo() == null ? ""
                        : authUser.getRawUserInfo().toJSONString(), 0, 1024))
                .setCode(code).setState(state);
        if (socialUser == null) {
            socialUserMapper.insert(data);
            return data;
        }
        socialUserMapper.updateById(data.setId(socialUser.getId()));
        return socialUserMapper.selectById(socialUser.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String bindSocialUser(SocialUserBindReqDTO reqDTO) {
        SocialUserDO socialUser = authSocialUser(reqDTO.getSocialType(), reqDTO.getUserType(),
                reqDTO.getCode(), reqDTO.getState());
        SocialUserBindDO bind = socialUserBindMapper.selectByUserTypeAndSocialUserId(reqDTO.getUserType(), socialUser.getId());
        if (bind != null) {
            if (!Objects.equals(bind.getUserId(), reqDTO.getUserId())) {
                // 不允许静默抢占：否则拿到他人授权码的人可以把对方的社交账号绑到自己名下
                throw exception(SOCIAL_USER_BIND_DUPLICATE);
            }
            return socialUser.getOpenid();
        }
        socialUserBindMapper.deleteByUserIdAndUserTypeAndSocialType(reqDTO.getUserId(), reqDTO.getUserType(),
                reqDTO.getSocialType());
        socialUserBindMapper.insert(new SocialUserBindDO().setUserId(reqDTO.getUserId()).setUserType(reqDTO.getUserType())
                .setSocialType(reqDTO.getSocialType()).setSocialUserId(socialUser.getId()));
        return socialUser.getOpenid();
    }

    @Override
    public void unbindSocialUser(SocialUserUnbindReqDTO reqDTO) {
        SocialUserDO socialUser = socialUserMapper.selectByTypeAndOpenid(reqDTO.getSocialType(), reqDTO.getOpenid());
        if (socialUser == null) {
            return;
        }
        socialUserBindMapper.deleteByUserIdAndUserTypeAndSocialUserId(reqDTO.getUserId(), reqDTO.getUserType(),
                socialUser.getId());
    }

    @Override
    public SocialUserRespDTO getSocialUserByUserId(Integer userType, Long userId, Integer socialType) {
        SocialUserBindDO bind = socialUserBindMapper.selectByUserIdAndUserTypeAndSocialType(userId, userType, socialType);
        if (bind == null) {
            return null;
        }
        SocialUserDO socialUser = socialUserMapper.selectById(bind.getSocialUserId());
        return socialUser == null ? null : toRespDTO(socialUser, userId);
    }

    @Override
    public SocialUserRespDTO getSocialUserByCode(Integer userType, Integer socialType, String code, String state) {
        SocialUserDO socialUser = authSocialUser(socialType, userType, code, state);
        SocialUserBindDO bind = socialUserBindMapper.selectByUserTypeAndSocialUserId(userType, socialUser.getId());
        return toRespDTO(socialUser, bind != null ? bind.getUserId() : null);
    }

    private static SocialUserRespDTO toRespDTO(SocialUserDO socialUser, Long userId) {
        SocialUserRespDTO respDTO = new SocialUserRespDTO();
        respDTO.setOpenid(socialUser.getOpenid());
        respDTO.setNickname(socialUser.getNickname());
        respDTO.setAvatar(socialUser.getAvatar());
        respDTO.setUserId(userId);
        return respDTO;
    }
}
