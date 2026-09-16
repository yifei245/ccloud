package com.ccloud.module.system.service.user;

import com.ccloud.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import jakarta.validation.Valid;

/**
 * 管理后台用户 Service
 */
public interface AdminUserService {

    /**
     * 创建用户：校验租户账号配额与账号、手机、邮箱唯一性，密码以 BCrypt 存储
     *
     * @return 用户编号
     */
    Long createUser(@Valid UserSaveReqVO createReqVO);

    AdminUserDO getUser(Long id);

    AdminUserDO getUserByUsername(String username);
}
