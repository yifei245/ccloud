package com.ccloud.module.member.api.user;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.api.user.dto.MemberUserRespDTO;
import com.ccloud.module.member.service.user.MemberUserService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class MemberUserApiImpl implements MemberUserApi {

    @Resource
    private MemberUserService userService;

    @Override
    public CommonResult<MemberUserRespDTO> getUser(Long id) {
        return success(BeanUtils.toBean(userService.getUser(id), MemberUserRespDTO.class));
    }

    @Override
    public CommonResult<List<MemberUserRespDTO>> getUserList(Collection<Long> ids) {
        return success(BeanUtils.toBean(userService.getUserList(ids), MemberUserRespDTO.class));
    }

    @Override
    public CommonResult<List<MemberUserRespDTO>> getUserListByNickname(String nickname) {
        return success(BeanUtils.toBean(userService.getUserListByNickname(nickname), MemberUserRespDTO.class));
    }

    @Override
    public CommonResult<MemberUserRespDTO> getUserByMobile(String mobile) {
        return success(BeanUtils.toBean(userService.getUserByMobile(mobile), MemberUserRespDTO.class));
    }

    @Override
    public CommonResult<Boolean> validateUser(Long id) {
        userService.validateUserExists(id);
        return success(true);
    }
}
