package com.ccloud.module.system.controller.admin.oauth2;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.system.controller.admin.oauth2.vo.user.OAuth2UserInfoRespVO;
import com.ccloud.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import com.ccloud.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * 第三方应用凭授权码换得的令牌访问用户信息，按 scope 控制
 */
@Tag(name = "管理后台 - OAuth2 用户信息")
@RestController
@RequestMapping("/admin-api/system/oauth2/user")
@Validated
public class OAuth2UserController {

    @Resource
    private AdminUserService userService;

    @GetMapping("/get")
    @Operation(summary = "获得用户基本信息", description = "需要 user.read 授权范围")
    @PreAuthorize("@ss.hasScope('user.read')")
    public CommonResult<OAuth2UserInfoRespVO> getUserInfo() {
        return success(BeanUtils.toBean(userService.getUser(SecurityFrameworkUtils.getLoginUserId()),
                OAuth2UserInfoRespVO.class));
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户基本信息", description = "需要 user.write 授权范围")
    @PreAuthorize("@ss.hasScope('user.write')")
    public CommonResult<Boolean> updateUserInfo(@Valid @RequestBody UserProfileUpdateReqVO reqVO) {
        userService.updateUserProfile(SecurityFrameworkUtils.getLoginUserId(), reqVO);
        return success(true);
    }
}
