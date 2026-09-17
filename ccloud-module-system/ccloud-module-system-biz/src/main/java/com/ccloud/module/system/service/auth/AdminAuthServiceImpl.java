package com.ccloud.module.system.service.auth;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS;
import static com.ccloud.module.system.enums.ErrorCodeConstants.AUTH_LOGIN_USER_DISABLED;

@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    /**
     * 账号不存在时用于比对的 BCrypt 哈希（对应一个随机口令），使耗时与"密码错误"一致
     */
    private static final String DUMMY_PASSWORD_HASH = "$2a$04$7qd8r0ZPj0DO0N5pU1Hz2uK7mV1h6dM2yJq9J9sQ8b7kQx1o3vY6e";

    @Resource
    private AdminUserService userService;

    @Override
    public AdminUserDO authenticate(String username, String password) {
        AdminUserDO user = userService.getUserByUsername(username);
        if (user == null) {
            // 账号不存在也执行一次比对，并返回与密码错误相同的错误码，避免被用来探测有效账号
            userService.isPasswordMatch(password, DUMMY_PASSWORD_HASH);
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        if (!userService.isPasswordMatch(password, user.getPassword())) {
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        if (CommonStatusEnum.isDisable(user.getStatus())) {
            throw exception(AUTH_LOGIN_USER_DISABLED);
        }
        return user;
    }
}
