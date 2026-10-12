package com.ccloud.framework.security.core.service;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SecurityFrameworkServiceImplTest {

    private PermissionFrameworkService permissionFrameworkService;
    private SecurityFrameworkServiceImpl service;

    @BeforeEach
    void setUp() {
        permissionFrameworkService = mock(PermissionFrameworkService.class);
        service = new SecurityFrameworkServiceImpl(permissionFrameworkService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId, List<String> scopes) {
        login(userId, UserTypeEnum.ADMIN, scopes);
    }

    private void login(Long userId, UserTypeEnum userType, List<String> scopes) {
        LoginUser user = new LoginUser();
        user.setId(userId);
        user.setUserType(userType.getValue());
        user.setTenantId(1L);
        user.setScopes(scopes);
        user.setExpiresTime(LocalDateTime.now().plusHours(1));
        SecurityFrameworkUtils.setLoginUser(user, null);
    }

    @Test
    @DisplayName("权限命中时返回 true")
    void hasPermission_shouldReturnTrueWhenGranted() {
        login(100L, null);
        when(permissionFrameworkService.hasAnyPermissions(eq(100L), any(String[].class))).thenReturn(true);
        assertThat(service.hasPermission("system:user:create")).isTrue();
    }

    @Test
    @DisplayName("权限未命中时返回 false")
    void hasPermission_shouldReturnFalseWhenDenied() {
        login(100L, null);
        when(permissionFrameworkService.hasAnyPermissions(eq(100L), any(String[].class))).thenReturn(false);
        assertThat(service.hasPermission("system:user:create")).isFalse();
    }

    @Test
    @DisplayName("未登录时一律返回 false，绝不因为没有身份就放行")
    void hasPermission_shouldReturnFalseWhenNotLoggedIn() {
        assertThat(service.hasPermission("system:user:create")).isFalse();
        verify(permissionFrameworkService, never()).hasAnyPermissions(any(), any(String[].class));
    }

    @Test
    @DisplayName("非管理员身份没有角色与权限：开放应用、会员的编号与管理员编号会重复，不能继承同编号管理员的权限")
    void nonAdminIdentity_shouldNeverHavePermissionsOrRoles() {
        for (UserTypeEnum type : List.of(UserTypeEnum.OPEN_APP, UserTypeEnum.MEMBER)) {
            login(1L, type, List.of("member.read"));
            assertThat(service.hasPermission("system:user:create")).isFalse();
            assertThat(service.hasRole("super_admin")).isFalse();
            assertThat(service.hasScope("member.read")).isTrue();
        }
        verify(permissionFrameworkService, never()).hasAnyPermissions(any(), any(String[].class));
        verify(permissionFrameworkService, never()).hasAnyRoles(any(), any(String[].class));
    }

    @Test
    @DisplayName("多权限任一命中即放行")
    void hasAnyPermissions_shouldPassWhenAnyGranted() {
        login(100L, null);
        when(permissionFrameworkService.hasAnyPermissions(eq(100L), any(String[].class))).thenReturn(true);
        assertThat(service.hasAnyPermissions("a:b:c", "d:e:f")).isTrue();
    }

    @Test
    @DisplayName("同一用户同一权限的重复判断应命中本地缓存，避免每次鉴权都发起一次 RPC")
    void hasPermission_shouldCacheWithinTtl() {
        login(100L, null);
        when(permissionFrameworkService.hasAnyPermissions(eq(100L), any(String[].class))).thenReturn(true);

        service.hasPermission("system:user:create");
        service.hasPermission("system:user:create");

        verify(permissionFrameworkService, times(1)).hasAnyPermissions(eq(100L), any(String[].class));
    }

    @Test
    @DisplayName("不同权限应各自独立判断，不能被缓存串味")
    void hasPermission_cacheKeyShouldIncludePermission() {
        login(100L, null);
        when(permissionFrameworkService.hasAnyPermissions(eq(100L), any(String[].class))).thenReturn(true);

        service.hasPermission("system:user:create");
        service.hasPermission("system:user:delete");

        verify(permissionFrameworkService, times(2)).hasAnyPermissions(eq(100L), any(String[].class));
    }

    @Test
    @DisplayName("角色判断应透传给权限服务")
    void hasAnyRoles_shouldDelegate() {
        login(100L, null);
        when(permissionFrameworkService.hasAnyRoles(eq(100L), any(String[].class))).thenReturn(true);
        assertThat(service.hasAnyRoles("super_admin")).isTrue();
    }

    @Test
    @DisplayName("scope 校验直接读令牌内容，开放 API 无需回查权限服务")
    void hasScope_shouldReadFromTokenWithoutRpc() {
        login(200L, List.of("user.read", "order.write"));

        assertThat(service.hasScope("user.read")).isTrue();
        assertThat(service.hasScope("user.write")).isFalse();
        assertThat(service.hasAnyScopes("nope", "order.write")).isTrue();
        verifyNoInteractions(permissionFrameworkService);
    }

    @Test
    @DisplayName("令牌没有任何 scope 时 scope 校验应返回 false")
    void hasScope_shouldReturnFalseWhenTokenHasNoScopes() {
        login(200L, null);
        assertThat(service.hasScope("user.read")).isFalse();
    }
}
