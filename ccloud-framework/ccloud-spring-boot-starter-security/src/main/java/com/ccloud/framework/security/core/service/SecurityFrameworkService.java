package com.ccloud.framework.security.core.service;

/**
 * 权限表达式服务，注册为名为 {@code ss} 的 Bean。
 *
 * <p>用法：{@code @PreAuthorize("@ss.hasPermission('system:user:create')")}
 */
public interface SecurityFrameworkService {

    boolean hasPermission(String permission);

    boolean hasAnyPermissions(String... permissions);

    boolean hasRole(String role);

    boolean hasAnyRoles(String... roles);

    /**
     * 校验令牌的授权范围，开放 API 用
     */
    boolean hasScope(String scope);

    boolean hasAnyScopes(String... scopes);
}
