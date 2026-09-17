package com.ccloud.module.system.service.auth;

import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;

/**
 * 管理后台认证 Service
 */
public interface AdminAuthService {

    /**
     * 校验账号密码
     *
     * @return 通过校验的用户
     */
    AdminUserDO authenticate(String username, String password);
}
