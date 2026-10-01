package com.ccloud.module.system.controller.admin.user;

import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.system.controller.admin.user.vo.profile.UserProfileRespVO;
import com.ccloud.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import com.ccloud.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import com.ccloud.module.system.dal.dataobject.dept.DeptDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.service.dept.DeptService;
import com.ccloud.module.system.service.dept.PostService;
import com.ccloud.module.system.service.permission.PermissionService;
import com.ccloud.module.system.service.permission.RoleService;
import com.ccloud.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.pojo.CommonResult.success;
import static com.ccloud.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;

/**
 * 个人中心：只操作当前登录用户，不需要额外权限标识
 */
@Tag(name = "管理后台 - 用户个人中心")
@RestController
@RequestMapping("/admin-api/system/user/profile")
@Validated
public class UserProfileController {

    @Resource
    private AdminUserService userService;
    @Resource
    private DeptService deptService;
    @Resource
    private PostService postService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleService roleService;

    @GetMapping("/get")
    @Operation(summary = "获得登录用户信息")
    public CommonResult<UserProfileRespVO> getUserProfile() {
        AdminUserDO user = userService.getUser(SecurityFrameworkUtils.getLoginUserId());
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        UserProfileRespVO respVO = BeanUtils.toBean(user, UserProfileRespVO.class);
        respVO.setRoles(CollectionUtils.convertList(
                roleService.getRoleList(permissionService.getUserRoleIdListByUserId(user.getId())),
                role -> item(role.getId(), role.getName())));
        if (user.getDeptId() != null) {
            DeptDO dept = deptService.getDept(user.getDeptId());
            if (dept != null) {
                respVO.setDept(item(dept.getId(), dept.getName()));
            }
        }
        respVO.setPosts(CollectionUtils.convertList(postService.getPostList(user.getPostIds()),
                post -> item(post.getId(), post.getName())));
        return success(respVO);
    }

    @PutMapping("/update")
    @Operation(summary = "修改用户个人信息")
    public CommonResult<Boolean> updateUserProfile(@Valid @RequestBody UserProfileUpdateReqVO reqVO) {
        userService.updateUserProfile(SecurityFrameworkUtils.getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping(value = "/update-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传用户个人头像")
    @ApiAccessLog(requestEnable = false)
    public CommonResult<String> updateUserAvatar(@RequestParam("avatarFile") MultipartFile file) throws IOException {
        return success(userService.updateUserAvatar(SecurityFrameworkUtils.getLoginUserId(),
                file.getOriginalFilename(), file.getBytes()));
    }

    @PutMapping("/update-password")
    @Operation(summary = "修改用户个人密码")
    public CommonResult<Boolean> updateUserProfilePassword(@Valid @RequestBody UserProfileUpdatePasswordReqVO reqVO) {
        userService.updateUserPassword(SecurityFrameworkUtils.getLoginUserId(), reqVO);
        return success(true);
    }

    private static UserProfileRespVO.Item item(Long id, String name) {
        UserProfileRespVO.Item item = new UserProfileRespVO.Item();
        item.setId(id);
        item.setName(name);
        return item;
    }
}
