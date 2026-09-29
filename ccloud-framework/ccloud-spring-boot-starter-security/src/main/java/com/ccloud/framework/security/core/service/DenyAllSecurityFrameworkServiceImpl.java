package com.ccloud.framework.security.core.service;

import lombok.extern.slf4j.Slf4j;

/**
 * 未提供权限数据来源时的权限表达式实现：一律拒绝，并提示补齐 {@link PermissionFrameworkService}。
 * 仅按 scope 判断的接口（客户端模式令牌）同样拒绝，因为无法确认调用方身份与权限的对应关系
 */
@Slf4j
public class DenyAllSecurityFrameworkServiceImpl implements SecurityFrameworkService {

    private void warn(String expression) {
        log.error("[{}] 未提供 PermissionFrameworkService 实现，权限校验一律拒绝。"
                + "system 服务应直接实现，其他服务经 PermissionApi 实现", expression);
    }

    @Override
    public boolean hasPermission(String permission) {
        warn("hasPermission");
        return false;
    }

    @Override
    public boolean hasAnyPermissions(String... permissions) {
        warn("hasAnyPermissions");
        return false;
    }

    @Override
    public boolean hasRole(String role) {
        warn("hasRole");
        return false;
    }

    @Override
    public boolean hasAnyRoles(String... roles) {
        warn("hasAnyRoles");
        return false;
    }

    @Override
    public boolean hasScope(String scope) {
        warn("hasScope");
        return false;
    }

    @Override
    public boolean hasAnyScopes(String... scopes) {
        warn("hasAnyScopes");
        return false;
    }
}
