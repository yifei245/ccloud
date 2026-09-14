package com.ccloud.module.system.api.permission;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.ccloud.module.system.api.permission.dto.DeptDataPermissionRespDTO;

import java.util.Collection;
import java.util.Set;

/**
 * 权限 RPC。供各业务服务的 {@code @ss} 权限表达式与数据权限规则回查。
 *
 * <p>集合参数用 {@link Collection} 而非可变参数：Feign 对集合的 GET 编码是稳定的
 * {@code ?permissions=a&permissions=b}，对数组的处理在不同版本间有差异。
 */
@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - 权限")
public interface PermissionApi {

    String PREFIX = ApiConstants.PREFIX + "/permission";

    @GetMapping(PREFIX + "/has-any-permissions")
    @Operation(summary = "判断用户是否拥有任意一个权限，超级管理员恒为 true")
    CommonResult<Boolean> hasAnyPermissions(@RequestParam("userId") Long userId,
                                            @RequestParam("permissions") Collection<String> permissions);

    @GetMapping(PREFIX + "/has-any-roles")
    @Operation(summary = "判断用户是否拥有任意一个角色")
    CommonResult<Boolean> hasAnyRoles(@RequestParam("userId") Long userId,
                                      @RequestParam("roles") Collection<String> roles);

    @GetMapping(PREFIX + "/get-dept-data-permission")
    @Operation(summary = "获得用户的部门数据权限，多角色时取最宽的一个")
    CommonResult<DeptDataPermissionRespDTO> getDeptDataPermission(@RequestParam("userId") Long userId);

    @GetMapping(PREFIX + "/user-role-id-list-by-role-id")
    @Operation(summary = "获得拥有指定角色的用户编号集合，工作流按角色分配审批人时使用")
    CommonResult<Set<Long>> getUserRoleIdListByRoleIds(@RequestParam("roleIds") Collection<Long> roleIds);
}
