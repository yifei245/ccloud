package com.ccloud.module.pay.framework.contract;

import com.ccloud.framework.security.core.service.PermissionFrameworkService;
import com.ccloud.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 权限表达式 {@code @ss} 的数据来源：经 RPC 由 system 判断。结果由 security starter 在本地缓存
 */
@Component
public class PermissionFrameworkServiceImpl implements PermissionFrameworkService {

    @Lazy
    @Resource
    private PermissionApi permissionApi;

    @Override
    public boolean hasAnyPermissions(Long userId, String... permissions) {
        return Boolean.TRUE.equals(permissionApi.hasAnyPermissions(userId, Arrays.asList(permissions)).getCheckedData());
    }

    @Override
    public boolean hasAnyRoles(Long userId, String... roles) {
        return Boolean.TRUE.equals(permissionApi.hasAnyRoles(userId, Arrays.asList(roles)).getCheckedData());
    }
}
