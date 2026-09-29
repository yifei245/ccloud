package com.ccloud.module.system.api.permission;

import com.ccloud.module.system.api.permission.dto.DeptDataPermissionRespDTO;
import com.ccloud.module.system.service.permission.PermissionService;
import com.ccloud.framework.common.pojo.CommonResult;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Set;

import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * 权限 RPC 实现：各业务服务的 @ss 权限表达式与数据权限规则依赖它
 */
@RestController
@Validated
public class PermissionApiImpl implements PermissionApi {

    @Resource
    private PermissionService permissionService;

    @Override
    public CommonResult<Boolean> hasAnyPermissions(Long userId, Collection<String> permissions) {
        return success(permissionService.hasAnyPermissions(userId, permissions.toArray(new String[0])));
    }

    @Override
    public CommonResult<Boolean> hasAnyRoles(Long userId, Collection<String> roles) {
        return success(permissionService.hasAnyRoles(userId, roles.toArray(new String[0])));
    }

    @Override
    public CommonResult<DeptDataPermissionRespDTO> getDeptDataPermission(Long userId) {
        // 服务返回框架数据权限规则使用的同名 DTO，这里转为 RPC 契约的 DTO
        var permission = permissionService.getDeptDataPermission(userId);
        DeptDataPermissionRespDTO respDTO = new DeptDataPermissionRespDTO();
        respDTO.setAll(permission.isAll());
        respDTO.setSelf(permission.isSelf());
        respDTO.setDeptIds(permission.getDeptIds());
        return success(respDTO);
    }

    @Override
    public CommonResult<Set<Long>> getUserRoleIdListByRoleIds(Collection<Long> roleIds) {
        return success(permissionService.getUserRoleIdListByRoleIds(roleIds));
    }
}
