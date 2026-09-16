package com.ccloud.module.system.service.user;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.dal.mysql.user.AdminUserMapper;
import com.ccloud.module.system.service.tenant.TenantService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class AdminUserServiceImpl implements AdminUserService {

    @Resource
    private AdminUserMapper userMapper;
    @Resource
    @Lazy
    private TenantService tenantService;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserSaveReqVO createReqVO) {
        tenantService.handleTenantInfo(tenant -> {
            long count = userMapper.selectCount();
            if (count >= tenant.getAccountCount()) {
                throw exception(USER_COUNT_MAX, tenant.getAccountCount());
            }
        });
        validateUserUnique(null, createReqVO.getUsername(), createReqVO.getMobile(), createReqVO.getEmail());

        AdminUserDO user = BeanUtils.toBean(createReqVO, AdminUserDO.class);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        user.setPassword(passwordEncoder.encode(createReqVO.getPassword()));
        userMapper.insert(user);
        return user.getId();
    }

    /**
     * 账号、手机、邮箱在租户内唯一；手机与邮箱为空时不参与校验
     */
    void validateUserUnique(Long id, String username, String mobile, String email) {
        AdminUserDO user = userMapper.selectByUsername(username);
        if (user != null && !user.getId().equals(id)) {
            throw exception(USER_USERNAME_EXISTS);
        }
        if (StrUtil.isNotEmpty(mobile)) {
            user = userMapper.selectByMobile(mobile);
            if (user != null && !user.getId().equals(id)) {
                throw exception(USER_MOBILE_EXISTS);
            }
        }
        if (StrUtil.isNotEmpty(email)) {
            user = userMapper.selectByEmail(email);
            if (user != null && !user.getId().equals(id)) {
                throw exception(USER_EMAIL_EXISTS);
            }
        }
    }

    @Override
    public AdminUserDO getUser(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public AdminUserDO getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }
}
