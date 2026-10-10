package com.ccloud.framework.security.core.service;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * 权限表达式实现。
 *
 * <p>带一层本地缓存：一次页面渲染可能触发几十次权限判断，
 * 每次都发 RPC 到 system 服务不现实。代价是权限变更后最多延迟
 * 一个缓存周期生效——system 模块在角色/菜单变更时会通过 Redis
 * 广播让各节点主动清理，把这个延迟压到可忽略。
 */
public class SecurityFrameworkServiceImpl implements SecurityFrameworkService {

    private final PermissionFrameworkService permissionFrameworkService;

    /**
     * key 为 {@code userId:类型:标识}，避免不同用户或不同权限之间串味
     */
    private final Cache<String, Boolean> cache;

    public SecurityFrameworkServiceImpl(PermissionFrameworkService permissionFrameworkService) {
        this(permissionFrameworkService, Duration.ofMinutes(10), 10_000L);
    }

    public SecurityFrameworkServiceImpl(PermissionFrameworkService permissionFrameworkService,
                                        Duration cacheDuration, long maxSize) {
        this.permissionFrameworkService = permissionFrameworkService;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(cacheDuration)
                .maximumSize(maxSize)
                .build();
    }

    /**
     * 清理指定用户的权限缓存。system 模块在权限变更时经 Redis 广播触发。
     */
    public void invalidate(Long userId) {
        if (userId == null) {
            cache.invalidateAll();
            return;
        }
        String prefix = userId + ":";
        cache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }

    @Override
    public boolean hasPermission(String permission) {
        return hasAnyPermissions(permission);
    }

    @Override
    public boolean hasAnyPermissions(String... permissions) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        // 没有登录身份就没有任何权限。这里绝不能"无身份即放行"
        if (userId == null || !isAdmin()) {
            return false;
        }
        String key = userId + ":p:" + String.join(",", permissions);
        return Boolean.TRUE.equals(cache.get(key,
                k -> permissionFrameworkService.hasAnyPermissions(userId, permissions)));
    }

    @Override
    public boolean hasRole(String role) {
        return hasAnyRoles(role);
    }

    @Override
    public boolean hasAnyRoles(String... roles) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null || !isAdmin()) {
            return false;
        }
        String key = userId + ":r:" + String.join(",", roles);
        return Boolean.TRUE.equals(cache.get(key,
                k -> permissionFrameworkService.hasAnyRoles(userId, roles)));
    }

    /**
     * 角色与权限是管理员体系的概念，按用户编号查询。会员、开放应用的编号与管理员编号会重复，
     * 若不限定用户类型，编号恰好相同的非管理员身份会继承该管理员的权限（例如 1 号即超级管理员）
     */
    private static boolean isAdmin() {
        return UserTypeEnum.isAdmin(SecurityFrameworkUtils.getLoginUserType());
    }

    @Override
    public boolean hasScope(String scope) {
        return hasAnyScopes(scope);
    }

    @Override
    public boolean hasAnyScopes(String... scopes) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return false;
        }
        List<String> granted = loginUser.getScopes();
        // scope 是令牌自带的内容，无需回查权限服务
        return CollUtil.isNotEmpty(granted) && Arrays.stream(scopes).anyMatch(granted::contains);
    }
}
