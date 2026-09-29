package com.ccloud.module.system.framework.contract;

import com.ccloud.framework.security.core.service.PermissionFrameworkService;
import com.ccloud.module.system.service.permission.PermissionService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 权限表达式 {@code @ss} 的数据来源：system 服务直接查本地权限数据
 */
@Component
public class PermissionFrameworkServiceImpl implements PermissionFrameworkService {

    /**
     * 延迟注入：框架在初始化 MyBatis 拦截器时就要创建本类，而这些 Service 依赖 Mapper，
     * 直接注入会形成循环依赖。首次调用时才真正取得 Service
     */
    @Lazy
    @Resource
    private PermissionService permissionService;

    @Override
    public boolean hasAnyPermissions(Long userId, String... permissions) {
        return permissionService.hasAnyPermissions(userId, permissions);
    }

    @Override
    public boolean hasAnyRoles(Long userId, String... roles) {
        return permissionService.hasAnyRoles(userId, roles);
    }
}
