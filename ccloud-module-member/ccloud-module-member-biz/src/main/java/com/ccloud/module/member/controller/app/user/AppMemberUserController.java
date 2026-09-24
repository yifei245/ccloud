package com.ccloud.module.member.controller.app.user;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.member.controller.app.user.vo.AppMemberUserInfoRespVO;
import com.ccloud.module.member.controller.app.user.vo.AppMemberUserUpdateMobileReqVO;
import com.ccloud.module.member.controller.app.user.vo.AppMemberUserUpdateReqVO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.service.user.MemberUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * 只能操作当前登录用户本人，用户编号一律取自登录身份而不是请求参数
 */
@Tag(name = "用户 App - 会员用户")
@RestController
@RequestMapping("/app-api/member/user")
@Validated
public class AppMemberUserController {

    @Resource
    private MemberUserService userService;

    @GetMapping("/get")
    @Operation(summary = "获得当前用户的基本信息")
    public CommonResult<AppMemberUserInfoRespVO> getUserInfo() {
        MemberUserDO user = userService.validateUserExists(SecurityFrameworkUtils.getLoginUserId());
        AppMemberUserInfoRespVO respVO = BeanUtils.toBean(user, AppMemberUserInfoRespVO.class);
        respVO.setPasswordSet(StrUtil.isNotEmpty(user.getPassword()));
        return success(respVO);
    }

    @PutMapping("/update")
    @Operation(summary = "修改基本信息")
    public CommonResult<Boolean> updateUser(@RequestBody @Valid AppMemberUserUpdateReqVO reqVO) {
        userService.updateUser(SecurityFrameworkUtils.getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping("/update-mobile")
    @Operation(summary = "修改手机号", description = "新旧手机号都要验证码")
    public CommonResult<Boolean> updateUserMobile(@RequestBody @Valid AppMemberUserUpdateMobileReqVO reqVO,
                                                  HttpServletRequest request) {
        userService.updateUserMobile(SecurityFrameworkUtils.getLoginUserId(), reqVO, ServletUtils.getClientIP(request));
        return success(true);
    }
}
