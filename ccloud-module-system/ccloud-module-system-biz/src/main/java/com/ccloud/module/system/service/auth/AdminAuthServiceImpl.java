package com.ccloud.module.system.service.auth;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.system.api.sms.dto.SmsCodeSendReqDTO;
import com.ccloud.module.system.api.sms.dto.SmsCodeUseReqDTO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthResetPasswordReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthSmsLoginReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthSmsSendReqVO;
import com.ccloud.module.system.enums.sms.SmsSceneEnum;
import com.ccloud.module.system.service.sms.SmsCodeService;
import com.ccloud.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import com.ccloud.module.system.controller.admin.auth.vo.AuthRegisterReqVO;
import com.ccloud.module.system.controller.admin.permission.vo.menu.MenuListReqVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import com.ccloud.module.system.dal.dataobject.logger.LoginLogDO;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import com.ccloud.module.system.dal.dataobject.permission.MenuDO;
import com.ccloud.module.system.dal.dataobject.permission.RoleDO;
import com.ccloud.module.system.dal.dataobject.tenant.TenantDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.dal.redis.auth.LoginFailCountRedisDAO;
import com.ccloud.module.system.enums.logger.LoginLogTypeEnum;
import com.ccloud.module.system.enums.logger.LoginResultEnum;
import com.ccloud.module.system.enums.permission.MenuTypeEnum;
import com.ccloud.module.system.service.captcha.CaptchaService;
import com.ccloud.module.system.service.logger.LoginLogService;
import com.ccloud.module.system.service.oauth2.OAuth2TokenService;
import com.ccloud.module.system.service.permission.MenuService;
import com.ccloud.module.system.service.permission.PermissionService;
import com.ccloud.module.system.service.permission.RoleService;
import com.ccloud.module.system.service.tenant.TenantService;
import com.ccloud.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.*;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class AdminAuthServiceImpl implements AdminAuthService {

    /**
     * 账号不存在时用于比对的 BCrypt 哈希（对应一个随机口令），使耗时与"密码错误"一致
     */
    private static final String DUMMY_PASSWORD_HASH = "$2a$04$7qd8r0ZPj0DO0N5pU1Hz2uK7mV1h6dM2yJq9J9sQ8b7kQx1o3vY6e";

    @Resource
    private AdminUserService userService;
    @Resource
    private RoleService roleService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private MenuService menuService;
    @Resource
    private TenantService tenantService;
    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private LoginLogService loginLogService;
    @Resource
    private CaptchaService captchaService;
    @Resource
    private LoginFailCountRedisDAO loginFailCountRedisDAO;
    @Resource
    private SmsCodeService smsCodeService;

    /**
     * 管理后台登录使用的 OAuth2 客户端
     */
    @Value("${ccloud.system.auth.client-id:ccloud-web}")
    private String clientId;
    @Value("${ccloud.system.auth.login-fail-limit:5}")
    private int loginFailLimit;
    @Value("${ccloud.system.auth.login-fail-lock-minutes:10}")
    private int loginFailLockMinutes;
    @Value("${ccloud.system.auth.register-enabled:false}")
    private boolean registerEnabled;

    // ========== 登录 ==========

    @Override
    public AuthLoginRespVO login(AuthLoginReqVO reqVO, String userIp, String userAgent) {
        // 验证码失败不计入登录失败次数：否则攻击者可以用错误的验证码把别人的账号锁死
        if (captchaService.isEnabled()) {
            try {
                captchaService.validate(reqVO.getCaptchaUuid(), reqVO.getCaptchaCode());
            } catch (RuntimeException ex) {
                createLoginLog(0L, reqVO.getUsername(), LoginLogTypeEnum.LOGIN_USERNAME,
                        LoginResultEnum.CAPTCHA_CODE_ERROR, userIp, userAgent);
                throw ex;
            }
        }
        Long tenantId = resolveTenantId(reqVO.getTenantName());
        return TenantUtils.execute(tenantId, () -> loginByUsername(reqVO, userIp, userAgent));
    }

    /**
     * 登录页可以按租户名登录；未传时沿用请求头中的租户编号
     */
    private Long resolveTenantId(String tenantName) {
        if (StrUtil.isEmpty(tenantName)) {
            return TenantContextHolder.getRequiredTenantId();
        }
        TenantDO tenant = tenantService.getTenantByName(tenantName);
        if (tenant == null) {
            throw exception(TENANT_NOT_EXISTS);
        }
        return tenant.getId();
    }

    private AuthLoginRespVO loginByUsername(AuthLoginReqVO reqVO, String userIp, String userAgent) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String username = reqVO.getUsername();
        if (loginFailCountRedisDAO.get(tenantId, username) >= loginFailLimit) {
            createLoginLog(0L, username, LoginLogTypeEnum.LOGIN_USERNAME, LoginResultEnum.BAD_CREDENTIALS,
                    userIp, userAgent);
            throw exception(AUTH_LOGIN_FAIL_LOCKED, loginFailLockMinutes);
        }

        AdminUserDO user;
        try {
            user = authenticate(username, reqVO.getPassword());
        } catch (RuntimeException ex) {
            loginFailCountRedisDAO.increment(tenantId, username, Duration.ofMinutes(loginFailLockMinutes));
            AdminUserDO failedUser = userService.getUserByUsername(username);
            LoginResultEnum result = isDisabled(ex) ? LoginResultEnum.USER_DISABLED : LoginResultEnum.BAD_CREDENTIALS;
            createLoginLog(failedUser != null ? failedUser.getId() : 0L, username,
                    LoginLogTypeEnum.LOGIN_USERNAME, result, userIp, userAgent);
            throw ex;
        }
        loginFailCountRedisDAO.delete(tenantId, username);
        return createTokenAfterLoginSuccess(user, LoginLogTypeEnum.LOGIN_USERNAME, userIp, userAgent);
    }

    private static boolean isDisabled(RuntimeException ex) {
        return ex instanceof com.ccloud.framework.common.exception.ServiceException serviceException
                && Objects.equals(serviceException.getCode(), AUTH_LOGIN_USER_DISABLED.getCode());
    }

    @Override
    public AdminUserDO authenticate(String username, String password) {
        AdminUserDO user = userService.getUserByUsername(username);
        if (user == null) {
            // 账号不存在也执行一次比对，并返回与密码错误相同的错误码，避免被用来探测有效账号
            userService.isPasswordMatch(password, DUMMY_PASSWORD_HASH);
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        if (!userService.isPasswordMatch(password, user.getPassword())) {
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        if (CommonStatusEnum.isDisable(user.getStatus())) {
            throw exception(AUTH_LOGIN_USER_DISABLED);
        }
        return user;
    }

    private AuthLoginRespVO createTokenAfterLoginSuccess(AdminUserDO user, LoginLogTypeEnum logType,
                                                         String userIp, String userAgent) {
        userService.updateUserLogin(user.getId(), userIp);
        createLoginLog(user.getId(), user.getUsername(), logType, LoginResultEnum.SUCCESS, userIp, userAgent);
        OAuth2AccessTokenDO accessToken = oauth2TokenService.createAccessToken(user.getId(),
                UserTypeEnum.ADMIN.getValue(), clientId, null);
        return buildLoginResp(accessToken);
    }

    private static AuthLoginRespVO buildLoginResp(OAuth2AccessTokenDO accessToken) {
        AuthLoginRespVO respVO = new AuthLoginRespVO();
        respVO.setUserId(accessToken.getUserId());
        respVO.setAccessToken(accessToken.getAccessToken());
        respVO.setRefreshToken(accessToken.getRefreshToken());
        respVO.setExpiresTime(accessToken.getExpiresTime());
        return respVO;
    }

    private void createLoginLog(Long userId, String username, LoginLogTypeEnum logType, LoginResultEnum result,
                                String userIp, String userAgent) {
        loginLogService.createLoginLog(new LoginLogDO().setLogType(logType.getType())
                .setUserId(userId).setUserType(UserTypeEnum.ADMIN.getValue()).setUsername(username)
                .setResult(result.getResult()).setUserIp(userIp == null ? "" : userIp)
                .setUserAgent(userAgent == null ? "" : userAgent));
    }

    // ========== 短信验证码 ==========

    /**
     * 管理后台可用的短信场景，会员场景由会员服务发送
     */
    private static final Set<Integer> ADMIN_SMS_SCENES = Set.of(SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene(),
            SmsSceneEnum.ADMIN_MEMBER_RESET_PASSWORD.getScene());

    @Override
    public void sendSmsCode(AuthSmsSendReqVO reqVO, String userIp) {
        if (!ADMIN_SMS_SCENES.contains(reqVO.getScene())) {
            throw invalidParamException("不支持的短信场景({})", reqVO.getScene());
        }
        // 短信按条计费，匿名接口不设图形验证码会被用来批量发短信
        if (captchaService.isEnabled()) {
            captchaService.validate(reqVO.getCaptchaUuid(), reqVO.getCaptchaCode());
        }
        if (userService.getUserByMobile(reqVO.getMobile()) == null) {
            throw exception(AUTH_MOBILE_NOT_EXISTS);
        }
        SmsCodeSendReqDTO sendReqDTO = new SmsCodeSendReqDTO();
        sendReqDTO.setMobile(reqVO.getMobile());
        sendReqDTO.setScene(reqVO.getScene());
        sendReqDTO.setCreateIp(userIp);
        smsCodeService.sendSmsCode(sendReqDTO);
    }

    @Override
    public AuthLoginRespVO smsLogin(AuthSmsLoginReqVO reqVO, String userIp, String userAgent) {
        try {
            smsCodeService.useSmsCode(buildSmsCodeUseReqDTO(reqVO.getMobile(), reqVO.getCode(),
                    SmsSceneEnum.ADMIN_MEMBER_LOGIN, userIp));
        } catch (RuntimeException ex) {
            createLoginLog(0L, reqVO.getMobile(), LoginLogTypeEnum.LOGIN_MOBILE, LoginResultEnum.CAPTCHA_CODE_ERROR,
                    userIp, userAgent);
            throw ex;
        }
        AdminUserDO user = userService.getUserByMobile(reqVO.getMobile());
        if (user == null) {
            throw exception(AUTH_MOBILE_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(user.getStatus())) {
            createLoginLog(user.getId(), user.getUsername(), LoginLogTypeEnum.LOGIN_MOBILE,
                    LoginResultEnum.USER_DISABLED, userIp, userAgent);
            throw exception(AUTH_LOGIN_USER_DISABLED);
        }
        return createTokenAfterLoginSuccess(user, LoginLogTypeEnum.LOGIN_MOBILE, userIp, userAgent);
    }

    @Override
    public void resetPassword(AuthResetPasswordReqVO reqVO, String userIp) {
        smsCodeService.useSmsCode(buildSmsCodeUseReqDTO(reqVO.getMobile(), reqVO.getCode(),
                SmsSceneEnum.ADMIN_MEMBER_RESET_PASSWORD, userIp));
        AdminUserDO user = userService.getUserByMobile(reqVO.getMobile());
        if (user == null) {
            throw exception(AUTH_MOBILE_NOT_EXISTS);
        }
        userService.resetUserPassword(user.getId(), reqVO.getPassword());
        // 忘记密码往往意味着密码可能已泄露，已登录的会话一并下线
        oauth2TokenService.removeAccessTokensByUser(user.getId(), UserTypeEnum.ADMIN.getValue());
    }

    private static SmsCodeUseReqDTO buildSmsCodeUseReqDTO(String mobile, String code, SmsSceneEnum scene,
                                                         String userIp) {
        SmsCodeUseReqDTO reqDTO = new SmsCodeUseReqDTO();
        reqDTO.setMobile(mobile);
        reqDTO.setCode(code);
        reqDTO.setScene(scene.getScene());
        reqDTO.setUsedIp(userIp);
        return reqDTO;
    }

    // ========== 注册 ==========

    @Override
    public AuthLoginRespVO register(AuthRegisterReqVO reqVO, String userIp, String userAgent) {
        if (!registerEnabled) {
            throw exception(AUTH_REGISTER_DISABLED);
        }
        if (captchaService.isEnabled()) {
            captchaService.validate(reqVO.getCaptchaUuid(), reqVO.getCaptchaCode());
        }
        UserSaveReqVO createReqVO = new UserSaveReqVO();
        createReqVO.setUsername(reqVO.getUsername());
        createReqVO.setNickname(reqVO.getNickname());
        createReqVO.setPassword(reqVO.getPassword());
        Long userId = userService.createUser(createReqVO);
        return createTokenAfterLoginSuccess(userService.getUser(userId), LoginLogTypeEnum.LOGIN_USERNAME,
                userIp, userAgent);
    }

    // ========== 登出与刷新 ==========

    @Override
    public void logout(String accessToken, Integer logType) {
        OAuth2AccessTokenDO token = oauth2TokenService.removeAccessToken(accessToken);
        if (token == null) {
            return;
        }
        if (!UserTypeEnum.ADMIN.getValue().equals(token.getUserType())) {
            return;
        }
        // 日志归属令牌所在租户，而不是发起登出请求时的租户
        TenantUtils.execute(token.getTenantId(), () -> {
            AdminUserDO user = userService.getUser(token.getUserId());
            loginLogService.createLoginLog(new LoginLogDO().setLogType(logType)
                    .setUserId(token.getUserId()).setUserType(token.getUserType())
                    .setUsername(user != null ? user.getUsername() : "")
                    .setResult(LoginResultEnum.SUCCESS.getResult()).setUserIp("").setUserAgent(""));
        });
    }

    @Override
    public AuthLoginRespVO refreshToken(String refreshToken) {
        return buildLoginResp(oauth2TokenService.refreshAccessToken(refreshToken, clientId));
    }

    // ========== 权限与菜单 ==========

    @Override
    public AuthPermissionInfoRespVO getPermissionInfo(Long userId) {
        AdminUserDO user = userService.getUser(userId);
        if (user == null) {
            return null;
        }
        // 只取开启的角色：停用角色的菜单与权限立即失效
        List<RoleDO> roles = CollectionUtils.filterList(
                roleService.getRoleListFromCache(permissionService.getUserRoleIdListByUserIdFromCache(userId)),
                role -> CommonStatusEnum.isEnable(role.getStatus()));
        Set<Long> roleIds = CollectionUtils.convertSet(roles, RoleDO::getId);

        // 菜单先按角色授权取，再按租户套餐与启用状态过滤，最后拆成"权限标识"与"菜单树"
        Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(roleIds);
        MenuListReqVO menuReqVO = new MenuListReqVO();
        menuReqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        List<MenuDO> menus = CollectionUtils.filterList(
                menuService.filterDisableMenus(menuService.getMenuListByTenant(menuReqVO)),
                menu -> menuIds.contains(menu.getId()));

        AuthPermissionInfoRespVO respVO = new AuthPermissionInfoRespVO();
        respVO.setUser(BeanUtils.toBean(user, AuthPermissionInfoRespVO.UserVO.class));
        respVO.setRoles(CollectionUtils.convertSet(roles, RoleDO::getCode));
        respVO.setPermissions(CollectionUtils.convertSet(
                CollectionUtils.filterList(menus, menu -> StrUtil.isNotEmpty(menu.getPermission())),
                MenuDO::getPermission));
        respVO.setMenus(buildMenuTree(menus));
        return respVO;
    }

    /**
     * 只有目录与菜单进入菜单树，按钮仅贡献权限标识；同级按 sort 升序
     */
    private static List<AuthPermissionInfoRespVO.MenuVO> buildMenuTree(List<MenuDO> menus) {
        List<MenuDO> routable = CollectionUtils.filterList(menus,
                menu -> !MenuTypeEnum.BUTTON.getType().equals(menu.getType()));
        routable.sort(Comparator.comparing(MenuDO::getSort, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MenuDO::getId));

        Map<Long, AuthPermissionInfoRespVO.MenuVO> nodes = new LinkedHashMap<>();
        routable.forEach(menu -> nodes.put(menu.getId(),
                BeanUtils.toBean(menu, AuthPermissionInfoRespVO.MenuVO.class)));

        List<AuthPermissionInfoRespVO.MenuVO> roots = new ArrayList<>();
        nodes.forEach((id, node) -> {
            AuthPermissionInfoRespVO.MenuVO parent = nodes.get(node.getParentId());
            // 父菜单未被授权（或已停用）时，子菜单上提为根，避免整块菜单不可见
            if (parent == null) {
                roots.add(node);
                return;
            }
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(node);
        });
        return roots;
    }
}
