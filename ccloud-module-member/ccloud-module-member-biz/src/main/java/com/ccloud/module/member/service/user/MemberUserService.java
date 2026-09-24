package com.ccloud.module.member.service.user;

import com.ccloud.module.member.controller.app.user.vo.AppMemberUserUpdateMobileReqVO;
import com.ccloud.module.member.controller.app.user.vo.AppMemberUserUpdateReqVO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;

import java.util.Collection;
import java.util.List;

public interface MemberUserService {

    MemberUserDO getUser(Long id);

    List<MemberUserDO> getUserList(Collection<Long> ids);

    List<MemberUserDO> getUserListByNickname(String nickname);

    MemberUserDO getUserByMobile(String mobile);

    /**
     * 按手机号获取用户，不存在时注册。短信登录与小程序一键登录使用
     *
     * @param terminal 注册终端，见 {@link com.ccloud.framework.common.enums.TerminalEnum}
     */
    MemberUserDO createUserIfAbsent(String mobile, String registerIp, Integer terminal);

    void updateUserLogin(Long id, String loginIp);

    boolean isPasswordMatch(String rawPassword, String encodedPassword);

    void updateUserPassword(Long id, String password);

    void updateUser(Long id, AppMemberUserUpdateReqVO reqVO);

    /**
     * 修改手机号：新旧手机号的验证码都要通过
     */
    void updateUserMobile(Long id, AppMemberUserUpdateMobileReqVO reqVO, String userIp);

    /**
     * @return 存在的用户
     * @throws com.ccloud.framework.common.exception.ServiceException 用户不存在时抛 USER_NOT_EXISTS
     */
    MemberUserDO validateUserExists(Long id);
}
