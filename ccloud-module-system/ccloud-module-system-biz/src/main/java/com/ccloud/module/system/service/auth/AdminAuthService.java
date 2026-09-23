package com.ccloud.module.system.service.auth;

import com.ccloud.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthRegisterReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthResetPasswordReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthSmsLoginReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthSmsSendReqVO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import jakarta.validation.Valid;

/**
 * 管理后台认证 Service
 */
public interface AdminAuthService {

    /**
     * 账号密码登录：校验验证码、失败次数锁定、账号密码，成功后签发令牌并记录登录日志
     */
    AuthLoginRespVO login(@Valid AuthLoginReqVO reqVO, String userIp, String userAgent);

    /**
     * 发送管理后台的短信验证码（登录、忘记密码），手机号须属于本租户的管理员；开启图形验证码时须先通过校验
     */
    void sendSmsCode(@Valid AuthSmsSendReqVO reqVO, String userIp);

    /**
     * 短信验证码登录。手机号未绑定管理员时拒绝，不自动注册
     */
    AuthLoginRespVO smsLogin(@Valid AuthSmsLoginReqVO reqVO, String userIp, String userAgent);

    /**
     * 短信验证码重置密码，并作废该用户已签发的全部令牌
     */
    void resetPassword(@Valid AuthResetPasswordReqVO reqVO, String userIp);

    /**
     * 注册并直接登录；是否开放注册由 ccloud.system.auth.register-enabled 控制
     */
    AuthLoginRespVO register(@Valid AuthRegisterReqVO reqVO, String userIp, String userAgent);

    /**
     * 登出：作废访问令牌并记录登出日志。令牌不存在时静默返回，保证幂等
     */
    void logout(String accessToken, Integer logType);

    AuthLoginRespVO refreshToken(String refreshToken);

    /**
     * @return 登录用户的角色、权限标识与菜单树；用户不存在返回 null
     */
    AuthPermissionInfoRespVO getPermissionInfo(Long userId);

    /**
     * 校验账号密码
     *
     * @return 通过校验的用户
     */
    AdminUserDO authenticate(String username, String password);
}
