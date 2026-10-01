package com.ccloud.module.system.service.auth;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.infra.api.config.ConfigApi;
import com.ccloud.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthRegisterReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthResetPasswordReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthSmsLoginReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthSmsSendReqVO;
import com.ccloud.module.system.api.sms.dto.SmsCodeSendReqDTO;
import com.ccloud.module.system.api.sms.dto.SmsCodeUseReqDTO;
import com.ccloud.module.system.enums.sms.SmsSceneEnum;
import com.ccloud.module.system.service.sms.SmsCodeService;
import com.ccloud.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import com.ccloud.module.system.dal.dataobject.dict.DictDataDO;
import com.ccloud.module.system.dal.dataobject.logger.LoginLogDO;
import com.ccloud.module.system.dal.dataobject.permission.MenuDO;
import com.ccloud.module.system.dal.dataobject.permission.RoleDO;
import com.ccloud.module.system.dal.dataobject.tenant.TenantDO;
import com.ccloud.module.system.dal.mysql.logger.LoginLogMapper;
import com.ccloud.module.system.dal.mysql.oauth2.OAuth2ClientMapper;
import com.ccloud.module.system.dal.mysql.permission.MenuMapper;
import com.ccloud.module.system.dal.mysql.permission.RoleMapper;
import com.ccloud.module.system.dal.mysql.tenant.TenantMapper;
import com.ccloud.module.system.dal.redis.auth.LoginFailCountRedisDAO;
import com.ccloud.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import com.ccloud.module.system.enums.logger.LoginLogTypeEnum;
import com.ccloud.module.system.enums.logger.LoginResultEnum;
import com.ccloud.module.system.enums.permission.MenuTypeEnum;
import com.ccloud.module.system.enums.permission.RoleCodeEnum;
import com.ccloud.module.system.enums.permission.RoleTypeEnum;
import com.ccloud.module.system.service.captcha.CaptchaService;
import com.ccloud.module.system.service.dept.DeptServiceImpl;
import com.ccloud.module.system.service.dept.PostServiceImpl;
import com.ccloud.module.system.service.dict.DictDataServiceImpl;
import com.ccloud.module.system.service.dict.DictTypeServiceImpl;
import com.ccloud.module.system.service.logger.LoginLogServiceImpl;
import com.ccloud.module.system.service.oauth2.*;
import com.ccloud.module.system.service.permission.*;
import com.ccloud.module.system.service.tenant.TenantService;
import com.ccloud.module.system.service.user.AdminUserService;
import com.ccloud.module.system.service.user.AdminUserServiceImpl;
import com.ccloud.module.system.test.BaseSystemDbAndRedisUnitTest;
import com.ccloud.module.system.test.CacheTestConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static com.ccloud.module.system.service.oauth2.OAuth2TestData.CLIENT_ID;
import static com.ccloud.module.system.service.oauth2.OAuth2TestData.client;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Import({AdminAuthServiceImpl.class, AdminUserServiceImpl.class, RoleServiceImpl.class, PermissionServiceImpl.class,
        MenuServiceImpl.class, DeptServiceImpl.class, PostServiceImpl.class, LoginLogServiceImpl.class,
        OAuth2TokenServiceImpl.class, OAuth2ClientServiceImpl.class, OAuth2AccessTokenRedisDAO.class,
        LoginFailCountRedisDAO.class, DictDataServiceImpl.class, DictTypeServiceImpl.class,
        CacheTestConfiguration.class, org.springframework.validation.beanvalidation.LocalValidatorFactoryBean.class})
@TestPropertySource(properties = "ccloud.system.auth.client-id=" + CLIENT_ID)
class AdminAuthServiceImplTest extends BaseSystemDbAndRedisUnitTest {

    @Resource
    private AdminAuthService authService;
    @Resource
    private AdminUserService userService;
    @Resource
    private RoleService roleService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private OAuth2TokenService tokenService;
    @Resource
    private LoginFailCountRedisDAO loginFailCountRedisDAO;
    @Resource
    private OAuth2ClientMapper clientMapper;
    @Resource
    private LoginLogMapper loginLogMapper;
    @Resource
    private com.ccloud.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper accessTokenMapper;
    @Resource
    private MenuMapper menuMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private TenantMapper tenantMapper;

    @MockitoBean
    private ConfigApi configApi;
    @MockitoBean
    private TenantService tenantService;
    @MockitoBean
    private CaptchaService captchaService;
    @MockitoBean
    private SmsCodeService smsCodeService;
    @MockitoBean
    private com.ccloud.module.system.service.social.SocialUserService socialUserService;
    @MockitoBean
    private com.ccloud.module.system.service.social.SocialClientService socialClientService;

    private static final String MOBILE = "13800000000";

    private static AuthRegisterReqVO newRegisterReqVO(String username) {
        AuthRegisterReqVO reqVO = new AuthRegisterReqVO();
        reqVO.setUsername(username);
        reqVO.setNickname("新用户");
        reqVO.setPassword("new123");
        return reqVO;
    }

    private Long userId;

    @BeforeEach
    void seed() {
        clientMapper.insert(client());
        UserSaveReqVO reqVO = new UserSaveReqVO();
        reqVO.setUsername("admin");
        reqVO.setNickname("管理员");
        reqVO.setPassword("admin123");
        reqVO.setMobile(MOBILE);
        userId = userService.createUser(reqVO);
        loginFailCountRedisDAO.delete(1L, "admin");
        when(captchaService.isEnabled()).thenReturn(false);
        when(configApi.getConfigValueByKey("system.auth.register-enabled")).thenReturn(CommonResult.success("true"));
        // 导入用户等路径用到的初始密码
        when(configApi.getConfigValueByKey("system.user.init-password")).thenReturn(CommonResult.success("init123"));
    }

    // ========== 账密登录 ==========

    @Test
    void login_成功时返回令牌并记录成功登录日志() {
        AuthLoginRespVO respVO = authService.login(loginReqVO("admin", "admin123"), "10.0.0.1", "JUnit");

        assertThat(respVO.getUserId()).isEqualTo(userId);
        assertThat(tokenService.checkAccessToken(respVO.getAccessToken()).getUserId()).isEqualTo(userId);
        assertThat(respVO.getRefreshToken()).isNotBlank();
        assertThat(respVO.getExpiresTime()).isAfter(LocalDateTime.now());

        LoginLogDO log = loginLogMapper.selectList().get(0);
        assertThat(log.getLogType()).isEqualTo(LoginLogTypeEnum.LOGIN_USERNAME.getType());
        assertThat(log.getResult()).isEqualTo(LoginResultEnum.SUCCESS.getResult());
        assertThat(log.getUsername()).isEqualTo("admin");
        assertThat(log.getUserId()).isEqualTo(userId);
        assertThat(log.getUserIp()).isEqualTo("10.0.0.1");
        assertThat(log.getUserType()).isEqualTo(UserTypeEnum.ADMIN.getValue());
        // 记录最后登录信息
        assertThat(userService.getUser(userId).getLoginIp()).isEqualTo("10.0.0.1");
    }

    @Test
    void login_用户名不存在应抛BAD_CREDENTIALS且日志记为失败() {
        assertServiceException(() -> authService.login(loginReqVO("nobody", "x"), "10.0.0.1", "JUnit"),
                AUTH_LOGIN_BAD_CREDENTIALS);

        LoginLogDO log = loginLogMapper.selectList().get(0);
        assertThat(log.getResult()).isEqualTo(LoginResultEnum.BAD_CREDENTIALS.getResult());
        assertThat(log.getUsername()).isEqualTo("nobody");
        assertThat(log.getUserId()).isZero();
    }

    @Test
    void login_密码错误应抛BAD_CREDENTIALS() {
        assertServiceException(() -> authService.login(loginReqVO("admin", "wrong"), "10.0.0.1", "JUnit"),
                AUTH_LOGIN_BAD_CREDENTIALS);
        assertThat(loginLogMapper.selectList().get(0).getResult())
                .isEqualTo(LoginResultEnum.BAD_CREDENTIALS.getResult());
    }

    @Test
    void login_用户被禁用应抛USER_DISABLED() {
        userService.updateUserStatus(userId, CommonStatusEnum.DISABLE.getStatus());

        assertServiceException(() -> authService.login(loginReqVO("admin", "admin123"), "10.0.0.1", "JUnit"),
                AUTH_LOGIN_USER_DISABLED);
        assertThat(loginLogMapper.selectList().get(0).getResult())
                .isEqualTo(LoginResultEnum.USER_DISABLED.getResult());
    }

    @Test
    void login_验证码错误应抛CAPTCHA_CODE_ERROR且不计入失败次数() {
        when(captchaService.isEnabled()).thenReturn(true);
        doThrow(exception(AUTH_LOGIN_CAPTCHA_CODE_ERROR, "验证码已过期"))
                .when(captchaService).validate(anyString(), anyString());

        AuthLoginReqVO reqVO = loginReqVO("admin", "admin123");
        reqVO.setCaptchaUuid("uuid-1");
        reqVO.setCaptchaCode("wrong");

        assertServiceException(() -> authService.login(reqVO, "10.0.0.1", "JUnit"), AUTH_LOGIN_CAPTCHA_CODE_ERROR);
        assertThat(loginLogMapper.selectList().get(0).getResult())
                .isEqualTo(LoginResultEnum.CAPTCHA_CODE_ERROR.getResult());
        assertThat(loginFailCountRedisDAO.get(1L, "admin")).isZero();
    }

    @Test
    void login_验证码开启时应校验验证码() {
        when(captchaService.isEnabled()).thenReturn(true);
        AuthLoginReqVO reqVO = loginReqVO("admin", "admin123");
        reqVO.setCaptchaUuid("uuid-1");
        reqVO.setCaptchaCode("1234");

        authService.login(reqVO, "10.0.0.1", "JUnit");

        verify(captchaService).validate("uuid-1", "1234");
    }

    @Test
    void login_连续失败5次后第6次应抛LOGIN_FAIL_LOCKED() {
        for (int i = 0; i < 5; i++) {
            assertServiceException(() -> authService.login(loginReqVO("admin", "wrong"), "10.0.0.1", "JUnit"),
                    AUTH_LOGIN_BAD_CREDENTIALS);
        }
        // 即使密码正确也被锁定
        assertServiceException(() -> authService.login(loginReqVO("admin", "admin123"), "10.0.0.1", "JUnit"),
                AUTH_LOGIN_FAIL_LOCKED);
    }

    @Test
    void login_成功后应重置失败计数() {
        assertServiceException(() -> authService.login(loginReqVO("admin", "wrong"), "10.0.0.1", "JUnit"),
                AUTH_LOGIN_BAD_CREDENTIALS);
        assertThat(loginFailCountRedisDAO.get(1L, "admin")).isEqualTo(1);

        authService.login(loginReqVO("admin", "admin123"), "10.0.0.1", "JUnit");

        assertThat(loginFailCountRedisDAO.get(1L, "admin")).isZero();
    }

    @Test
    void login_失败计数按租户与账号分别统计() {
        assertServiceException(() -> authService.login(loginReqVO("admin", "wrong"), "10.0.0.1", "JUnit"),
                AUTH_LOGIN_BAD_CREDENTIALS);
        assertThat(loginFailCountRedisDAO.get(1L, "admin")).isEqualTo(1);
        assertThat(loginFailCountRedisDAO.get(2L, "admin")).isZero();
    }

    @Test
    void login_多租户下应按tenantName解析出正确的tenantId() {
        // 显式指定编号：空库单独运行本类时自增编号恰为 1，会与当前租户重合
        TenantDO otherTenant = new TenantDO().setId(TENANT_ID + 1000).setName("其他公司").setContactName("联系人").setStatus(0)
                .setPackageId(1L).setExpireTime(LocalDateTime.now().plusYears(1)).setAccountCount(10);
        tenantMapper.insert(otherTenant);
        when(tenantService.getTenantByName("其他公司")).thenReturn(otherTenant);
        // 其他租户下的同名账号
        Long otherUserId = TenantUtils.execute(otherTenant.getId(), () -> {
            UserSaveReqVO reqVO = new UserSaveReqVO();
            reqVO.setUsername("admin");
            reqVO.setNickname("其他公司管理员");
            reqVO.setPassword("other123");
            return userService.createUser(reqVO);
        });

        AuthLoginReqVO reqVO = loginReqVO("admin", "other123");
        reqVO.setTenantName("其他公司");
        AuthLoginRespVO respVO = authService.login(reqVO, "10.0.0.1", "JUnit");

        assertThat(respVO.getUserId()).isEqualTo(otherUserId);
        assertThat(tokenService.checkAccessToken(respVO.getAccessToken()).getTenantId()).isEqualTo(otherTenant.getId());
    }

    @Test
    void login_tenantName不存在时应抛租户不存在() {
        when(tenantService.getTenantByName("不存在")).thenReturn(null);
        AuthLoginReqVO reqVO = loginReqVO("admin", "admin123");
        reqVO.setTenantName("不存在");
        assertServiceException(() -> authService.login(reqVO, "10.0.0.1", "JUnit"), TENANT_NOT_EXISTS);
    }

    // ========== 登出与刷新 ==========

    @Test
    void logout_应作废accessToken并记录登出日志() {
        AuthLoginRespVO respVO = authService.login(loginReqVO("admin", "admin123"), "10.0.0.1", "JUnit");
        loginLogMapper.delete(LoginLogDO::getUserId, userId);

        authService.logout(respVO.getAccessToken(), LoginLogTypeEnum.LOGOUT_SELF.getType());

        assertThat(tokenService.getAccessToken(respVO.getAccessToken())).isNull();
        LoginLogDO log = loginLogMapper.selectList().get(0);
        assertThat(log.getLogType()).isEqualTo(LoginLogTypeEnum.LOGOUT_SELF.getType());
        assertThat(log.getUserId()).isEqualTo(userId);
    }

    @Test
    void logout_令牌不存在时应静默成功_保证幂等() {
        authService.logout("not-exists", LoginLogTypeEnum.LOGOUT_SELF.getType());
        assertThat(loginLogMapper.selectCount()).isZero();
    }

    @Test
    void refreshToken_应签发新的访问令牌() {
        AuthLoginRespVO login = authService.login(loginReqVO("admin", "admin123"), "10.0.0.1", "JUnit");

        AuthLoginRespVO refreshed = authService.refreshToken(login.getRefreshToken());

        assertThat(refreshed.getAccessToken()).isNotEqualTo(login.getAccessToken());
        assertThat(tokenService.checkAccessToken(refreshed.getAccessToken()).getUserId()).isEqualTo(userId);
    }

    // ========== 注册 ==========

    @Test
    void register_成功并签发令牌() {
        AuthRegisterReqVO reqVO = new AuthRegisterReqVO();
        reqVO.setUsername("newbie");
        reqVO.setNickname("新用户");
        reqVO.setPassword("new123");

        AuthLoginRespVO respVO = authService.register(reqVO, "10.0.0.1", "JUnit");

        assertThat(userService.getUserByUsername("newbie")).isNotNull();
        assertThat(tokenService.checkAccessToken(respVO.getAccessToken()).getUserId()).isEqualTo(respVO.getUserId());
    }

    @Test
    void register_开关关闭或infra不可用时拒绝() {
        AuthRegisterReqVO reqVO = newRegisterReqVO("reg_off");
        when(configApi.getConfigValueByKey("system.auth.register-enabled")).thenReturn(CommonResult.success("false"));
        assertServiceException(() -> authService.register(reqVO, "127.0.0.1", "ut"), AUTH_REGISTER_DISABLED);
        when(configApi.getConfigValueByKey("system.auth.register-enabled")).thenThrow(new RuntimeException("down"));
        assertServiceException(() -> authService.register(reqVO, "127.0.0.1", "ut"), AUTH_REGISTER_DISABLED);
        assertThat(userService.getUserByUsername("reg_off")).isNull();
    }

    @Test
    void register_账号已存在() {
        AuthRegisterReqVO reqVO = new AuthRegisterReqVO();
        reqVO.setUsername("admin");
        reqVO.setNickname("重复");
        reqVO.setPassword("new123");
        assertServiceException(() -> authService.register(reqVO, "10.0.0.1", "JUnit"), USER_USERNAME_EXISTS);
    }

    // ========== 权限与菜单 ==========

    @Test
    void getPermissionInfo_超管应返回全部菜单与权限() {
        MenuDO dir = insertMenu("系统管理", 0L, MenuTypeEnum.DIR, "", 1);
        MenuDO page = insertMenu("用户管理", dir.getId(), MenuTypeEnum.MENU, "", 1);
        insertMenu("用户新增", page.getId(), MenuTypeEnum.BUTTON, "system:user:create", 1);
        Long roleId = insertSuperAdminRole();
        permissionService.assignUserRole(userId, Set.of(roleId));

        AuthPermissionInfoRespVO respVO = authService.getPermissionInfo(userId);

        assertThat(respVO.getUser().getId()).isEqualTo(userId);
        assertThat(respVO.getUser().getNickname()).isEqualTo("管理员");
        assertThat(respVO.getRoles()).containsExactly(RoleCodeEnum.SUPER_ADMIN.getCode());
        assertThat(respVO.getPermissions()).containsExactly("system:user:create");
        assertThat(respVO.getMenus()).extracting(AuthPermissionInfoRespVO.MenuVO::getName).containsExactly("系统管理");
        assertThat(respVO.getMenus().get(0).getChildren()).extracting(AuthPermissionInfoRespVO.MenuVO::getName)
                .containsExactly("用户管理");
    }

    @Test
    void getPermissionInfo_普通用户只返回其角色对应的菜单与权限() {
        MenuDO dir = insertMenu("系统管理", 0L, MenuTypeEnum.DIR, "", 1);
        MenuDO userPage = insertMenu("用户管理", dir.getId(), MenuTypeEnum.MENU, "", 1);
        MenuDO createButton = insertMenu("用户新增", userPage.getId(), MenuTypeEnum.BUTTON, "system:user:create", 1);
        MenuDO rolePage = insertMenu("角色管理", dir.getId(), MenuTypeEnum.MENU, "", 2);
        insertMenu("角色新增", rolePage.getId(), MenuTypeEnum.BUTTON, "system:role:create", 1);
        Long roleId = roleService.createRole(roleReqVO("运营", "operator"), RoleTypeEnum.CUSTOM.getType());
        permissionService.assignRoleMenu(roleId, Set.of(dir.getId(), userPage.getId(), createButton.getId()));
        permissionService.assignUserRole(userId, Set.of(roleId));

        AuthPermissionInfoRespVO respVO = authService.getPermissionInfo(userId);

        assertThat(respVO.getRoles()).containsExactly("operator");
        assertThat(respVO.getPermissions()).containsExactly("system:user:create");
        assertThat(respVO.getMenus().get(0).getChildren()).extracting(AuthPermissionInfoRespVO.MenuVO::getName)
                .containsExactly("用户管理");
    }

    @Test
    void getPermissionInfo_菜单应为树形且按sort排序_按钮不出现在树中() {
        MenuDO dir = insertMenu("系统管理", 0L, MenuTypeEnum.DIR, "", 1);
        insertMenu("角色管理", dir.getId(), MenuTypeEnum.MENU, "", 2);
        insertMenu("用户管理", dir.getId(), MenuTypeEnum.MENU, "", 1);
        insertMenu("用户新增", dir.getId(), MenuTypeEnum.BUTTON, "system:user:create", 0);
        Long roleId = insertSuperAdminRole();
        permissionService.assignUserRole(userId, Set.of(roleId));

        AuthPermissionInfoRespVO respVO = authService.getPermissionInfo(userId);

        assertThat(respVO.getMenus().get(0).getChildren()).extracting(AuthPermissionInfoRespVO.MenuVO::getName)
                .containsExactly("用户管理", "角色管理");
        assertThat(respVO.getPermissions()).contains("system:user:create");
    }

    @Test
    void getPermissionInfo_停用的菜单与角色不返回() {
        MenuDO dir = insertMenu("系统管理", 0L, MenuTypeEnum.DIR, "", 1);
        MenuDO disabled = insertMenu("停用页面", dir.getId(), MenuTypeEnum.MENU, "", 2);
        menuMapper.updateById(new MenuDO().setId(disabled.getId()).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        insertMenu("用户管理", dir.getId(), MenuTypeEnum.MENU, "", 1);
        Long roleId = insertSuperAdminRole();
        Long disabledRole = roleService.createRole(roleReqVO("停用角色", "off"), RoleTypeEnum.CUSTOM.getType());
        roleService.updateRoleStatus(disabledRole, CommonStatusEnum.DISABLE.getStatus());
        permissionService.assignUserRole(userId, Set.of(roleId, disabledRole));

        AuthPermissionInfoRespVO respVO = authService.getPermissionInfo(userId);

        assertThat(respVO.getRoles()).containsExactly(RoleCodeEnum.SUPER_ADMIN.getCode());
        assertThat(respVO.getMenus().get(0).getChildren()).extracting(AuthPermissionInfoRespVO.MenuVO::getName)
                .containsExactly("用户管理");
    }

    @Test
    void getPermissionInfo_用户不存在返回null() {
        assertThat(authService.getPermissionInfo(-1L)).isNull();
    }

    // ========== 短信验证码：发送、登录、重置密码 ==========

    @Test
    void sendSmsCode_手机号已注册时按场景发送() {
        authService.sendSmsCode(smsSendReqVO(MOBILE, SmsSceneEnum.ADMIN_MEMBER_LOGIN), "10.0.0.1");

        verify(smsCodeService).sendSmsCode(argThat((SmsCodeSendReqDTO dto) -> MOBILE.equals(dto.getMobile())
                && SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene().equals(dto.getScene())
                && "10.0.0.1".equals(dto.getCreateIp())));
    }

    @Test
    void sendSmsCode_手机号未注册应拒绝且不发送() {
        assertServiceException(() -> authService.sendSmsCode(smsSendReqVO("13900000000", SmsSceneEnum.ADMIN_MEMBER_LOGIN),
                "10.0.0.1"), AUTH_MOBILE_NOT_EXISTS);
        verifyNoInteractions(smsCodeService);
    }

    @Test
    void sendSmsCode_只允许管理后台场景() {
        assertThatThrownBy(() -> authService.sendSmsCode(smsSendReqVO(MOBILE, SmsSceneEnum.MEMBER_LOGIN), "10.0.0.1"))
                .isInstanceOf(com.ccloud.framework.common.exception.ServiceException.class);
        verifyNoInteractions(smsCodeService);
    }

    @Test
    void sendSmsCode_开启图形验证码时须先通过校验_防止被刷短信() {
        when(captchaService.isEnabled()).thenReturn(true);
        doThrow(exception(AUTH_LOGIN_CAPTCHA_CODE_ERROR, "验证码不正确")).when(captchaService).validate(any(), any());

        assertServiceException(() -> authService.sendSmsCode(smsSendReqVO(MOBILE, SmsSceneEnum.ADMIN_MEMBER_LOGIN),
                "10.0.0.1"), AUTH_LOGIN_CAPTCHA_CODE_ERROR);
        verifyNoInteractions(smsCodeService);
    }

    @Test
    void smsLogin_验证码正确时签发令牌并记录短信登录日志() {
        AuthLoginRespVO respVO = authService.smsLogin(smsLoginReqVO("123456"), "10.0.0.1", "JUnit");

        assertThat(tokenService.checkAccessToken(respVO.getAccessToken()).getUserId()).isEqualTo(userId);
        verify(smsCodeService).useSmsCode(argThat((SmsCodeUseReqDTO dto) -> MOBILE.equals(dto.getMobile())
                && "123456".equals(dto.getCode())
                && SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene().equals(dto.getScene())
                && "10.0.0.1".equals(dto.getUsedIp())));
        LoginLogDO log = loginLogMapper.selectList().get(0);
        assertThat(log.getLogType()).isEqualTo(LoginLogTypeEnum.LOGIN_MOBILE.getType());
        assertThat(log.getResult()).isEqualTo(LoginResultEnum.SUCCESS.getResult());
        assertThat(log.getUsername()).isEqualTo("admin");
    }

    @Test
    void smsLogin_验证码错误应抛异常且不签发() {
        doThrow(exception(SMS_CODE_NOT_FOUND)).when(smsCodeService).useSmsCode(any());

        assertServiceException(() -> authService.smsLogin(smsLoginReqVO("000000"), "10.0.0.1", "JUnit"),
                SMS_CODE_NOT_FOUND);
        assertThat(accessTokenMapper.selectCount()).as("不应签发令牌").isZero();
        LoginLogDO log = loginLogMapper.selectList().get(0);
        assertThat(log.getResult()).isEqualTo(LoginResultEnum.CAPTCHA_CODE_ERROR.getResult());
        assertThat(log.getLogType()).isEqualTo(LoginLogTypeEnum.LOGIN_MOBILE.getType());
    }

    @Test
    void smsLogin_手机号未注册应拒绝_管理员账号不自动注册() {
        AuthSmsLoginReqVO reqVO = smsLoginReqVO("123456");
        reqVO.setMobile("13900000000");
        assertServiceException(() -> authService.smsLogin(reqVO, "10.0.0.1", "JUnit"), AUTH_MOBILE_NOT_EXISTS);
    }

    @Test
    void smsLogin_用户被禁用应拒绝() {
        userService.updateUserStatus(userId, CommonStatusEnum.DISABLE.getStatus());
        assertServiceException(() -> authService.smsLogin(smsLoginReqVO("123456"), "10.0.0.1", "JUnit"),
                AUTH_LOGIN_USER_DISABLED);
    }

    @Test
    void resetPassword_验证码正确时修改密码并作废已有令牌() {
        String oldToken = authService.login(loginReqVO("admin", "admin123"), "10.0.0.1", "JUnit").getAccessToken();
        AuthResetPasswordReqVO reqVO = new AuthResetPasswordReqVO();
        reqVO.setMobile(MOBILE);
        reqVO.setCode("123456");
        reqVO.setPassword("newpass1");

        authService.resetPassword(reqVO, "10.0.0.1");

        verify(smsCodeService).useSmsCode(argThat((SmsCodeUseReqDTO dto) ->
                SmsSceneEnum.ADMIN_MEMBER_RESET_PASSWORD.getScene().equals(dto.getScene())));
        assertThat(authService.authenticate("admin", "newpass1").getId()).isEqualTo(userId);
        assertThat(tokenService.getAccessToken(oldToken)).isNull();
    }

    @Test
    void resetPassword_验证码错误时不修改密码() {
        doThrow(exception(SMS_CODE_NOT_FOUND)).when(smsCodeService).useSmsCode(any());
        AuthResetPasswordReqVO reqVO = new AuthResetPasswordReqVO();
        reqVO.setMobile(MOBILE);
        reqVO.setCode("000000");
        reqVO.setPassword("newpass1");

        assertServiceException(() -> authService.resetPassword(reqVO, "10.0.0.1"), SMS_CODE_NOT_FOUND);
        assertThat(authService.authenticate("admin", "admin123").getId()).isEqualTo(userId);
    }

    // ========== 社交登录 ==========

    private static final Integer DING = com.ccloud.module.system.enums.social.SocialTypeEnum.DINGTALK.getType();

    @Test
    void socialLogin_已绑定时签发令牌并记录社交登录日志() {
        when(socialUserService.getSocialUserByCode(UserTypeEnum.ADMIN.getValue(), DING, "code", "state"))
                .thenReturn(socialUser(userId));

        AuthLoginRespVO respVO = authService.socialLogin(socialLoginReqVO(), "10.0.0.1", "JUnit");

        assertThat(tokenService.checkAccessToken(respVO.getAccessToken()).getUserId()).isEqualTo(userId);
        LoginLogDO log = loginLogMapper.selectList().get(0);
        assertThat(log.getLogType()).isEqualTo(LoginLogTypeEnum.LOGIN_SOCIAL.getType());
        assertThat(log.getResult()).isEqualTo(LoginResultEnum.SUCCESS.getResult());
    }

    @Test
    void socialLogin_未绑定时提示绑定且不签发() {
        when(socialUserService.getSocialUserByCode(UserTypeEnum.ADMIN.getValue(), DING, "code", "state"))
                .thenReturn(socialUser(null));

        assertServiceException(() -> authService.socialLogin(socialLoginReqVO(), "10.0.0.1", "JUnit"),
                AUTH_THIRD_LOGIN_NOT_BIND);
        assertThat(accessTokenMapper.selectCount()).isZero();
    }

    @Test
    void socialLogin_绑定的用户已禁用应拒绝() {
        userService.updateUserStatus(userId, CommonStatusEnum.DISABLE.getStatus());
        when(socialUserService.getSocialUserByCode(UserTypeEnum.ADMIN.getValue(), DING, "code", "state"))
                .thenReturn(socialUser(userId));

        assertServiceException(() -> authService.socialLogin(socialLoginReqVO(), "10.0.0.1", "JUnit"),
                AUTH_LOGIN_USER_DISABLED);
    }

    @Test
    void socialBindLogin_账密正确时绑定社交账号并签发令牌() {
        when(socialUserService.getSocialUserByCode(UserTypeEnum.ADMIN.getValue(), DING, "code", "state"))
                .thenReturn(socialUser(null));

        AuthLoginRespVO respVO = authService.socialBindLogin(socialBindLoginReqVO("admin123"), "10.0.0.1", "JUnit");

        assertThat(respVO.getUserId()).isEqualTo(userId);
        verify(socialUserService).bindSocialUser(argThat(dto -> userId.equals(dto.getUserId())
                && DING.equals(dto.getSocialType()) && "code".equals(dto.getCode())
                && UserTypeEnum.ADMIN.getValue().equals(dto.getUserType())));
    }

    @Test
    void socialBindLogin_账密错误时不绑定() {
        when(socialUserService.getSocialUserByCode(UserTypeEnum.ADMIN.getValue(), DING, "code", "state"))
                .thenReturn(socialUser(null));

        assertServiceException(() -> authService.socialBindLogin(socialBindLoginReqVO("wrong"), "10.0.0.1", "JUnit"),
                AUTH_LOGIN_BAD_CREDENTIALS);
        verify(socialUserService, never()).bindSocialUser(any());
    }

    @Test
    void socialBindLogin_社交账号已绑定他人时直接拒绝_不校验账密也不签发() {
        when(socialUserService.getSocialUserByCode(UserTypeEnum.ADMIN.getValue(), DING, "code", "state"))
                .thenReturn(socialUser(999L));

        assertServiceException(() -> authService.socialBindLogin(socialBindLoginReqVO("admin123"), "10.0.0.1", "JUnit"),
                SOCIAL_USER_BIND_DUPLICATE);
        assertThat(accessTokenMapper.selectCount()).isZero();
    }

    private static com.ccloud.module.system.api.social.dto.SocialUserRespDTO socialUser(Long boundUserId) {
        var dto = new com.ccloud.module.system.api.social.dto.SocialUserRespDTO();
        dto.setOpenid("open-1");
        dto.setUserId(boundUserId);
        return dto;
    }

    private static com.ccloud.module.system.controller.admin.auth.vo.AuthSocialLoginReqVO socialLoginReqVO() {
        var reqVO = new com.ccloud.module.system.controller.admin.auth.vo.AuthSocialLoginReqVO();
        reqVO.setType(DING);
        reqVO.setCode("code");
        reqVO.setState("state");
        return reqVO;
    }

    private static com.ccloud.module.system.controller.admin.auth.vo.AuthSocialBindLoginReqVO socialBindLoginReqVO(String password) {
        var reqVO = new com.ccloud.module.system.controller.admin.auth.vo.AuthSocialBindLoginReqVO();
        reqVO.setType(DING);
        reqVO.setCode("code");
        reqVO.setState("state");
        reqVO.setUsername("admin");
        reqVO.setPassword(password);
        return reqVO;
    }

    // ========== 工具 ==========

    private static com.ccloud.framework.common.exception.ServiceException exception(
            com.ccloud.framework.common.exception.ErrorCode errorCode, Object... params) {
        return com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception(errorCode, params);
    }

    private Long insertSuperAdminRole() {
        RoleDO role = new RoleDO().setName("超级管理员").setCode(RoleCodeEnum.SUPER_ADMIN.getCode()).setSort(0)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setType(RoleTypeEnum.SYSTEM.getType()).setDataScope(1);
        roleMapper.insert(role);
        return role.getId();
    }

    private MenuDO insertMenu(String name, Long parentId, MenuTypeEnum type, String permission, int sort) {
        MenuDO menu = new MenuDO().setName(name).setParentId(parentId).setType(type.getType())
                .setPermission(permission).setSort(sort).setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setVisible(true).setKeepAlive(true).setAlwaysShow(true).setPath("/" + name).setComponent("c");
        menuMapper.insert(menu);
        return menu;
    }

    private static RoleSaveReqVO roleReqVO(String name, String code) {
        RoleSaveReqVO reqVO = new RoleSaveReqVO();
        reqVO.setName(name);
        reqVO.setCode(code);
        reqVO.setSort(1);
        return reqVO;
    }

    private static AuthLoginReqVO loginReqVO(String username, String password) {
        AuthLoginReqVO reqVO = new AuthLoginReqVO();
        reqVO.setUsername(username);
        reqVO.setPassword(password);
        return reqVO;
    }

    private static AuthSmsSendReqVO smsSendReqVO(String mobile, SmsSceneEnum scene) {
        AuthSmsSendReqVO reqVO = new AuthSmsSendReqVO();
        reqVO.setMobile(mobile);
        reqVO.setScene(scene.getScene());
        return reqVO;
    }

    private static AuthSmsLoginReqVO smsLoginReqVO(String code) {
        AuthSmsLoginReqVO reqVO = new AuthSmsLoginReqVO();
        reqVO.setMobile(MOBILE);
        reqVO.setCode(code);
        return reqVO;
    }
}
