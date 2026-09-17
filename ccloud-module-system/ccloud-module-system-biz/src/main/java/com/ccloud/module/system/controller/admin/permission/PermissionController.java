package com.ccloud.module.system.controller.admin.permission;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.system.controller.admin.permission.vo.permission.PermissionAssignRoleDataScopeReqVO;
import com.ccloud.module.system.controller.admin.permission.vo.permission.PermissionAssignRoleMenuReqVO;
import com.ccloud.module.system.controller.admin.permission.vo.permission.PermissionAssignUserRoleReqVO;
import com.ccloud.module.system.service.permission.PermissionService;
import com.ccloud.module.system.service.permission.RoleService;
import com.ccloud.module.system.service.tenant.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * 权限分配：角色-菜单、角色-数据范围、用户-角色
 */
@Tag(name = "管理后台 - 权限")
@RestController
@RequestMapping("/admin-api/system/permission")
@Validated
public class PermissionController {

    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleService roleService;
    @Resource
    private TenantService tenantService;

    @GetMapping("/list-role-menus")
    @Operation(summary = "获得角色拥有的菜单编号")
    @Parameter(name = "roleId", description = "角色编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-menu')")
    public CommonResult<Set<Long>> getRoleMenuList(@RequestParam("roleId") Long roleId) {
        return success(permissionService.getRoleMenuListByRoleId(roleId));
    }

    @PostMapping("/assign-role-menu")
    @Operation(summary = "赋予角色菜单", description = "超出当前租户套餐范围的菜单会被忽略")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-menu')")
    public CommonResult<Boolean> assignRoleMenu(@Valid @RequestBody PermissionAssignRoleMenuReqVO reqVO) {
        Set<Long> menuIds = new LinkedHashSet<>(CollUtil.emptyIfNull(reqVO.getMenuIds()));
        // 租户只能分配其套餐内的菜单，防止通过构造请求越过套餐限制
        tenantService.handleTenantMenu(tenantMenuIds -> menuIds.removeIf(id -> !tenantMenuIds.contains(id)));
        permissionService.assignRoleMenu(reqVO.getRoleId(), menuIds);
        return success(true);
    }

    @PostMapping("/assign-role-data-scope")
    @Operation(summary = "赋予角色数据权限")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-data-scope')")
    public CommonResult<Boolean> assignRoleDataScope(@Valid @RequestBody PermissionAssignRoleDataScopeReqVO reqVO) {
        roleService.updateRoleDataScope(reqVO.getRoleId(), reqVO.getDataScope(), reqVO.getDataScopeDeptIds());
        return success(true);
    }

    @GetMapping("/list-user-roles")
    @Operation(summary = "获得用户拥有的角色编号列表")
    @Parameter(name = "userId", description = "用户编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-user-role')")
    public CommonResult<Set<Long>> listUserRoles(@RequestParam("userId") Long userId) {
        return success(permissionService.getUserRoleIdListByUserId(userId));
    }

    @PostMapping("/assign-user-role")
    @Operation(summary = "赋予用户角色", description = "只有超级管理员才能分配超级管理员角色")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-user-role')")
    public CommonResult<Boolean> assignUserRole(@Valid @RequestBody PermissionAssignUserRoleReqVO reqVO) {
        roleService.validateRoleList(reqVO.getRoleIds());
        permissionService.validateAssignableRoles(SecurityFrameworkUtils.getLoginUserId(), reqVO.getRoleIds());
        permissionService.assignUserRole(reqVO.getUserId(), reqVO.getRoleIds());
        return success(true);
    }
}
