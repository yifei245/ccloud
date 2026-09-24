package com.ccloud.module.member.service.user;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.member.controller.app.user.vo.AppMemberUserUpdateMobileReqVO;
import com.ccloud.module.member.controller.app.user.vo.AppMemberUserUpdateReqVO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import com.ccloud.module.system.api.sms.SmsCodeApi;
import com.ccloud.module.system.api.sms.dto.SmsCodeUseReqDTO;
import com.ccloud.module.system.enums.sms.SmsSceneEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.member.enums.ErrorCodeConstants.USER_MOBILE_USED;
import static com.ccloud.module.member.enums.ErrorCodeConstants.USER_NOT_EXISTS;

@Service
@Validated
@Slf4j
public class MemberUserServiceImpl implements MemberUserService {

    @Resource
    private MemberUserMapper memberUserMapper;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private SmsCodeApi smsCodeApi;

    @Override
    public MemberUserDO getUser(Long id) {
        return memberUserMapper.selectById(id);
    }

    @Override
    public List<MemberUserDO> getUserList(Collection<Long> ids) {
        return memberUserMapper.selectList(MemberUserDO::getId, ids);
    }

    @Override
    public List<MemberUserDO> getUserListByNickname(String nickname) {
        return memberUserMapper.selectListByNicknameLike(nickname);
    }

    @Override
    public MemberUserDO getUserByMobile(String mobile) {
        return memberUserMapper.selectByMobile(mobile);
    }

    @Override
    public MemberUserDO createUserIfAbsent(String mobile, String registerIp, Integer terminal) {
        MemberUserDO user = memberUserMapper.selectByMobile(mobile);
        if (user != null) {
            return user;
        }
        user = new MemberUserDO().setMobile(mobile).setPassword("")
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setRegisterIp(StrUtil.nullToEmpty(registerIp)).setRegisterTerminal(terminal)
                // 昵称会展示给其他用户（评价、拼团），不能直接用手机号
                .setNickname("用户" + StrUtil.subSuf(mobile, mobile.length() - 4))
                .setAvatar("");
        try {
            memberUserMapper.insert(user);
            return user;
        } catch (DuplicateKeyException e) {
            // 同一手机号并发注册（重复点击、多端同时登录），以先写入的为准
            log.info("[createUserIfAbsent] 手机号 {} 并发注册，复用已存在的用户", StrUtil.hide(mobile, 3, 7));
            return memberUserMapper.selectByMobile(mobile);
        }
    }

    @Override
    public void updateUserLogin(Long id, String loginIp) {
        memberUserMapper.updateById(new MemberUserDO().setId(id)
                .setLoginIp(StrUtil.nullToEmpty(loginIp)).setLoginDate(LocalDateTime.now()));
    }

    @Override
    public boolean isPasswordMatch(String rawPassword, String encodedPassword) {
        // 短信、小程序注册的用户没有密码，空串不能被任何输入匹配
        if (StrUtil.isEmpty(encodedPassword) || rawPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public void updateUserPassword(Long id, String password) {
        validateUserExists(id);
        memberUserMapper.updateById(new MemberUserDO().setId(id).setPassword(passwordEncoder.encode(password)));
    }

    @Override
    public void updateUser(Long id, AppMemberUserUpdateReqVO reqVO) {
        validateUserExists(id);
        // updateById 忽略 null 字段，未传的字段保持原值
        memberUserMapper.updateById(new MemberUserDO().setId(id).setNickname(reqVO.getNickname())
                .setAvatar(reqVO.getAvatar()).setSex(reqVO.getSex()).setBirthday(reqVO.getBirthday()));
    }

    @Override
    public void updateUserMobile(Long id, AppMemberUserUpdateMobileReqVO reqVO, String userIp) {
        MemberUserDO user = validateUserExists(id);
        // 先查占用再消费验证码：否则号码已被占用时验证码白白作废，用户只能重新获取
        if (memberUserMapper.selectByMobile(reqVO.getMobile()) != null) {
            throw exception(USER_MOBILE_USED, reqVO.getMobile());
        }
        useSmsCode(user.getMobile(), reqVO.getOldCode(), userIp);
        useSmsCode(reqVO.getMobile(), reqVO.getCode(), userIp);
        memberUserMapper.updateById(new MemberUserDO().setId(id).setMobile(reqVO.getMobile()));
    }

    private void useSmsCode(String mobile, String code, String userIp) {
        SmsCodeUseReqDTO reqDTO = new SmsCodeUseReqDTO();
        reqDTO.setMobile(mobile);
        reqDTO.setCode(code);
        reqDTO.setScene(SmsSceneEnum.MEMBER_UPDATE_MOBILE.getScene());
        reqDTO.setUsedIp(userIp);
        smsCodeApi.useSmsCode(reqDTO).checkError();
    }

    @Override
    public MemberUserDO validateUserExists(Long id) {
        MemberUserDO user = id == null ? null : memberUserMapper.selectById(id);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        return user;
    }
}
