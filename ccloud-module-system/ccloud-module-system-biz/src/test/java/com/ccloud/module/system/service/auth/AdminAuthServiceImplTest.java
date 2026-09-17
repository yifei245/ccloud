package com.ccloud.module.system.service.auth;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static com.ccloud.module.system.enums.ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS;
import static com.ccloud.module.system.enums.ErrorCodeConstants.AUTH_LOGIN_USER_DISABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceImplTest {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @InjectMocks
    private AdminAuthServiceImpl authService;
    @Mock
    private AdminUserService userService;

    @Test
    void authenticate_成功() {
        AdminUserDO user = user(CommonStatusEnum.ENABLE);
        when(userService.getUserByUsername("admin")).thenReturn(user);
        when(userService.isPasswordMatch(eq("admin123"), anyString()))
                .thenAnswer(inv -> ENCODER.matches(inv.getArgument(0), inv.getArgument(1)));

        assertThat(authService.authenticate("admin", "admin123")).isSameAs(user);
    }

    /**
     * 账号不存在与密码错误返回同一错误码，避免被用来探测有效账号
     */
    @Test
    void authenticate_账号不存在与密码错误返回相同错误码() {
        when(userService.getUserByUsername("nobody")).thenReturn(null);
        assertThatThrownBy(() -> authService.authenticate("nobody", "x"))
                .extracting("code").isEqualTo(AUTH_LOGIN_BAD_CREDENTIALS.getCode());

        when(userService.getUserByUsername("admin")).thenReturn(user(CommonStatusEnum.ENABLE));
        when(userService.isPasswordMatch(anyString(), anyString())).thenReturn(false);
        assertThatThrownBy(() -> authService.authenticate("admin", "wrong"))
                .extracting("code").isEqualTo(AUTH_LOGIN_BAD_CREDENTIALS.getCode());
    }

    /**
     * 账号不存在时仍执行一次密码比对，使响应耗时与"密码错误"一致，防止按耗时探测账号
     */
    @Test
    void authenticate_账号不存在时仍执行密码比对() {
        when(userService.getUserByUsername("nobody")).thenReturn(null);
        assertThatThrownBy(() -> authService.authenticate("nobody", "x"));
        verify(userService).isPasswordMatch(eq("x"), anyString());
    }

    @Test
    void authenticate_账号已禁用() {
        when(userService.getUserByUsername("admin")).thenReturn(user(CommonStatusEnum.DISABLE));
        when(userService.isPasswordMatch(anyString(), anyString())).thenReturn(true);
        assertThatThrownBy(() -> authService.authenticate("admin", "admin123"))
                .extracting("code").isEqualTo(AUTH_LOGIN_USER_DISABLED.getCode());
    }

    private static AdminUserDO user(CommonStatusEnum status) {
        return new AdminUserDO().setId(1L).setUsername("admin").setPassword(ENCODER.encode("admin123"))
                .setStatus(status.getStatus());
    }
}
