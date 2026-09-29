package com.ccloud.module.system.api.user;

import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.api.user.dto.AdminUserRespDTO;
import com.ccloud.module.system.service.user.AdminUserService;
import com.ccloud.framework.common.pojo.CommonResult;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * 管理员用户 RPC 实现。返回 DTO 不含密码等敏感字段
 */
@RestController
@Validated
public class AdminUserApiImpl implements AdminUserApi {

    @Resource
    private AdminUserService userService;

    @Override
    public CommonResult<AdminUserRespDTO> getUser(Long id) {
        return success(BeanUtils.toBean(userService.getUser(id), AdminUserRespDTO.class));
    }

    @Override
    public CommonResult<List<AdminUserRespDTO>> getUserList(Collection<Long> ids) {
        return success(BeanUtils.toBean(userService.getUserList(ids), AdminUserRespDTO.class));
    }

    @Override
    public CommonResult<List<AdminUserRespDTO>> getUserListByDeptIds(Collection<Long> deptIds) {
        return success(BeanUtils.toBean(userService.getUserListByDeptIds(deptIds), AdminUserRespDTO.class));
    }

    @Override
    public CommonResult<List<AdminUserRespDTO>> getUserListByPostIds(Collection<Long> postIds) {
        return success(BeanUtils.toBean(userService.getUserListByPostIds(postIds), AdminUserRespDTO.class));
    }

    @Override
    public CommonResult<Boolean> validateUserList(Collection<Long> ids) {
        userService.validateUserList(ids);
        return success(true);
    }
}
