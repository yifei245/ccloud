package com.ccloud.module.system.api;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.web.core.handler.GlobalExceptionHandler;
import com.ccloud.module.infra.api.config.ConfigApi;
import com.ccloud.module.system.api.dept.DeptApiImpl;
import com.ccloud.module.system.api.dept.PostApiImpl;
import com.ccloud.module.system.api.dict.DictDataApiImpl;
import com.ccloud.module.system.api.logger.LoginLogApiImpl;
import com.ccloud.module.system.api.oauth2.OAuth2TokenApiImpl;
import com.ccloud.module.system.api.permission.PermissionApiImpl;
import com.ccloud.module.system.api.permission.RoleApiImpl;
import com.ccloud.module.system.api.tenant.TenantApiImpl;
import com.ccloud.module.system.api.user.AdminUserApiImpl;
import com.ccloud.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import com.ccloud.module.system.dal.dataobject.dict.DictDataDO;
import com.ccloud.module.system.dal.dataobject.logger.LoginLogDO;
import com.ccloud.module.system.dal.dataobject.permission.RoleDO;
import com.ccloud.module.system.dal.dataobject.tenant.TenantDO;
import com.ccloud.module.system.dal.mysql.dict.DictDataMapper;
import com.ccloud.module.system.dal.mysql.logger.LoginLogMapper;
import com.ccloud.module.system.dal.mysql.oauth2.OAuth2ClientMapper;
import com.ccloud.module.system.dal.mysql.permission.RoleMapper;
import com.ccloud.module.system.dal.mysql.tenant.TenantMapper;
import com.ccloud.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import com.ccloud.module.system.enums.permission.DataScopeEnum;
import com.ccloud.module.system.enums.permission.RoleCodeEnum;
import com.ccloud.module.system.enums.permission.RoleTypeEnum;
import com.ccloud.module.system.service.dept.DeptService;
import com.ccloud.module.system.service.dept.DeptServiceImpl;
import com.ccloud.module.system.service.dept.PostServiceImpl;
import com.ccloud.module.system.service.dict.DictDataServiceImpl;
import com.ccloud.module.system.service.dict.DictTypeServiceImpl;
import com.ccloud.module.system.service.logger.LoginLogServiceImpl;
import com.ccloud.module.system.service.oauth2.OAuth2ClientServiceImpl;
import com.ccloud.module.system.service.oauth2.OAuth2TokenService;
import com.ccloud.module.system.service.oauth2.OAuth2TokenServiceImpl;
import com.ccloud.module.system.service.permission.MenuServiceImpl;
import com.ccloud.module.system.service.permission.PermissionService;
import com.ccloud.module.system.service.permission.PermissionServiceImpl;
import com.ccloud.module.system.service.permission.RoleServiceImpl;
import com.ccloud.module.system.service.tenant.TenantServiceImpl;
import com.ccloud.module.system.service.user.AdminUserService;
import com.ccloud.module.system.service.user.AdminUserServiceImpl;
import com.ccloud.module.system.test.BaseSystemDbAndRedisUnitTest;
import com.ccloud.module.system.test.CacheTestConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.*;

import static com.ccloud.module.system.service.oauth2.OAuth2TestData.CLIENT_ID;
import static com.ccloud.module.system.service.oauth2.OAuth2TestData.client;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RPC 接口：以 HTTP 调用校验 Feign 接口上声明的路径、参数绑定与错误响应，业务走真实服务与数据库。
 * 调用方经 Feign 按接口注解发请求，这里的路径与参数名都取自接口定义
 */
@Import({OAuth2TokenApiImpl.class, PermissionApiImpl.class, RoleApiImpl.class, AdminUserApiImpl.class,
        DeptApiImpl.class, PostApiImpl.class, DictDataApiImpl.class, TenantApiImpl.class, LoginLogApiImpl.class,
        OAuth2TokenServiceImpl.class, OAuth2ClientServiceImpl.class, OAuth2AccessTokenRedisDAO.class,
        PermissionServiceImpl.class, RoleServiceImpl.class, MenuServiceImpl.class, AdminUserServiceImpl.class,
        DeptServiceImpl.class, PostServiceImpl.class, DictDataServiceImpl.class, DictTypeServiceImpl.class,
        TenantServiceImpl.class, LoginLogServiceImpl.class,
        CacheTestConfiguration.class, org.springframework.validation.beanvalidation.LocalValidatorFactoryBean.class})
class RpcApiIntegrationTest extends BaseSystemDbAndRedisUnitTest {

    /**
     * 服务尚未实现、随对应任务提供 RPC 实现的接口。实现落地后须从此处移除，否则本测试失败
     */
    private static final Map<String, String> PENDING_APIS = Map.of();

    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private OAuth2TokenService tokenService;
    @Resource
    private AdminUserService userService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private DeptService deptService;
    @Resource
    private OAuth2ClientMapper clientMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private DictDataMapper dictDataMapper;
    @Resource
    private TenantMapper tenantMapper;
    @Resource
    private LoginLogMapper loginLogMapper;

    @MockitoBean
    private com.ccloud.module.system.service.tenant.TenantPackageService tenantPackageService;
    @MockitoBean
    private ConfigApi configApi;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(configApi.getConfigValueByKey("system.user.init-password")).thenReturn(CommonResult.success("init123"));
        Object[] controllers = applicationContext.getBeansWithAnnotation(RestController.class).values().toArray();
        mockMvc = MockMvcBuilders.standaloneSetup(controllers)
                .setControllerAdvice(new GlobalExceptionHandler("ccloud-system-server", null))
                .build();
        clientMapper.insert(client());
        // 创建用户时校验当前租户的账号配额
        tenantMapper.insert(new TenantDO().setId(TENANT_ID).setName("rpc-当前租户").setContactName("联系人")
                .setContactMobile("13800000000").setStatus(CommonStatusEnum.ENABLE.getStatus()).setPackageId(0L)
                .setExpireTime(LocalDateTime.now().plusDays(1)).setAccountCount(100).setWebsite(""));
    }

    // ========== OAuth2 令牌：网关对每个请求调用 ==========

    @Test
    void checkAccessToken_有效令牌应返回完整用户信息() throws Exception {
        Long userId = createUser("rpc-admin", null);
        String token = tokenService.createAccessToken(userId, UserTypeEnum.ADMIN.getValue(), CLIENT_ID, List.of("user.read"))
                .getAccessToken();

        mockMvc.perform(get("/rpc-api/system/oauth2/token/check").param("accessToken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.userType").value(UserTypeEnum.ADMIN.getValue()))
                .andExpect(jsonPath("$.data.tenantId").value(TENANT_ID))
                .andExpect(jsonPath("$.data.clientId").value(CLIENT_ID))
                .andExpect(jsonPath("$.data.scopes", contains("user.read")))
                .andExpect(jsonPath("$.data.userInfo.nickname").value("昵称rpc-admin"))
                .andExpect(jsonPath("$.data.expiresTime").isNotEmpty());
    }

    @Test
    void checkAccessToken_无效令牌应返回401错误码而非抛异常() throws Exception {
        mockMvc.perform(get("/rpc-api/system/oauth2/token/check").param("accessToken", "not-exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void removeAccessTokensByUser_应作废该用户在所有客户端的令牌_不影响其他用户与其他用户类型() throws Exception {
        String mine = tokenService.createAccessToken(9001L, UserTypeEnum.MEMBER.getValue(), CLIENT_ID, null)
                .getAccessToken();
        String sameIdAdmin = tokenService.createAccessToken(9001L, UserTypeEnum.ADMIN.getValue(), CLIENT_ID, null)
                .getAccessToken();
        String other = tokenService.createAccessToken(9002L, UserTypeEnum.MEMBER.getValue(), CLIENT_ID, null)
                .getAccessToken();

        mockMvc.perform(delete("/rpc-api/system/oauth2/token/remove-by-user")
                        .param("userId", "9001").param("userType", UserTypeEnum.MEMBER.getValue().toString()))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/rpc-api/system/oauth2/token/check").param("accessToken", mine))
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/rpc-api/system/oauth2/token/check").param("accessToken", sameIdAdmin))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/rpc-api/system/oauth2/token/check").param("accessToken", other))
                .andExpect(jsonPath("$.code").value(0));
    }

    // ========== 权限 ==========

    @Test
    void hasAnyPermissions_超管返回true() throws Exception {
        Long userId = createUser("rpc-super", null);
        RoleDO role = new RoleDO().setName("超级管理员").setCode(RoleCodeEnum.SUPER_ADMIN.getCode()).setSort(0)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setType(RoleTypeEnum.SYSTEM.getType())
                .setDataScope(DataScopeEnum.ALL.getScope());
        roleMapper.insert(role);
        permissionService.assignUserRole(userId, Set.of(role.getId()));
        Long plainUserId = createUser("rpc-plain", null);

        // 集合参数以逗号拼接传递，与 Feign 编码一致
        mockMvc.perform(get("/rpc-api/system/permission/has-any-permissions")
                        .param("userId", String.valueOf(userId)).param("permissions", "system:user:create,system:user:delete"))
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(get("/rpc-api/system/permission/has-any-permissions")
                        .param("userId", String.valueOf(plainUserId)).param("permissions", "system:user:create"))
                .andExpect(jsonPath("$.data").value(false));
        mockMvc.perform(get("/rpc-api/system/permission/has-any-roles")
                        .param("userId", String.valueOf(userId)).param("roles", RoleCodeEnum.SUPER_ADMIN.getCode(), "other"))
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(get("/rpc-api/system/permission/user-role-id-list-by-role-id").param("roleIds", String.valueOf(role.getId())))
                .andExpect(jsonPath("$.data", contains(userId.intValue())));
    }

    @Test
    void getDeptDataPermission_返回与角色配置一致的数据权限() throws Exception {
        Long deptId = createDept("研发部", 0L);
        Long userId = createUser("rpc-dept", deptId);
        RoleDO role = new RoleDO().setName("部门主管").setCode("dept_leader").setSort(1)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setType(RoleTypeEnum.CUSTOM.getType())
                .setDataScope(DataScopeEnum.DEPT_CUSTOM.getScope()).setDataScopeDeptIds(Set.of(900L));
        roleMapper.insert(role);
        permissionService.assignUserRole(userId, Set.of(role.getId()));

        mockMvc.perform(get("/rpc-api/system/permission/get-dept-data-permission").param("userId", String.valueOf(userId)))
                .andExpect(jsonPath("$.data.all").value(false))
                .andExpect(jsonPath("$.data.self").value(false))
                .andExpect(jsonPath("$.data.deptIds", containsInAnyOrder(900, deptId.intValue())));
    }

    @Test
    void validRoleList_角色不存在应返回对应错误码() throws Exception {
        mockMvc.perform(get("/rpc-api/system/role/valid").param("ids", "-1"))
                .andExpect(jsonPath("$.code").value(com.ccloud.module.system.enums.ErrorCodeConstants.ROLE_NOT_EXISTS.getCode()));
    }

    // ========== 用户、部门、岗位 ==========

    @Test
    void getUser_不应返回密码() throws Exception {
        Long userId = createUser("rpc-user", null);
        mockMvc.perform(get("/rpc-api/system/user/get").param("id", String.valueOf(userId)))
                .andExpect(jsonPath("$.data.username").value("rpc-user"))
                .andExpect(jsonPath("$.data.nickname").value("昵称rpc-user"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
        mockMvc.perform(get("/rpc-api/system/user/list").param("ids", userId + ",-1"))
                .andExpect(jsonPath("$.data[*].id", contains(userId.intValue())));
        mockMvc.perform(get("/rpc-api/system/user/valid").param("ids", String.valueOf(userId)))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getChildDeptList_返回全部后代部门() throws Exception {
        Long root = createDept("总部", 0L);
        Long child = createDept("华东", root);
        Long grandChild = createDept("上海", child);
        mockMvc.perform(get("/rpc-api/system/dept/list-child").param("id", String.valueOf(root)))
                .andExpect(jsonPath("$.data[*].id", containsInAnyOrder(child.intValue(), grandChild.intValue())));
        mockMvc.perform(get("/rpc-api/system/dept/get").param("id", String.valueOf(child)))
                .andExpect(jsonPath("$.data.parentId").value(root));
    }

    // ========== 字典、租户、日志 ==========

    @Test
    void getDictData_按值与按标签均返回正确数据() throws Exception {
        dictDataMapper.insert(new DictDataDO().setDictType("system_user_sex").setLabel("男").setValue("1").setSort(1)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setColorType("").setCssClass("").setRemark(""));

        mockMvc.perform(get("/rpc-api/system/dict-data/get").param("dictType", "system_user_sex").param("value", "1"))
                .andExpect(jsonPath("$.data.label").value("男"));
        mockMvc.perform(get("/rpc-api/system/dict-data/parse").param("dictType", "system_user_sex").param("label", "男"))
                .andExpect(jsonPath("$.data.value").value("1"));
        mockMvc.perform(get("/rpc-api/system/dict-data/get").param("dictType", "system_user_sex").param("value", "9"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getTenantIdList_只返回开启且未过期的租户() throws Exception {
        TenantDO ok = insertTenant("rpc-正常", CommonStatusEnum.ENABLE, LocalDateTime.now().plusDays(1));
        TenantDO disabled = insertTenant("rpc-禁用", CommonStatusEnum.DISABLE, LocalDateTime.now().plusDays(1));
        TenantDO expired = insertTenant("rpc-过期", CommonStatusEnum.ENABLE, LocalDateTime.now().minusDays(1));

        mockMvc.perform(get("/rpc-api/system/tenant/id-list"))
                .andExpect(jsonPath("$.data", hasItem(ok.getId().intValue())))
                .andExpect(jsonPath("$.data", not(hasItem(disabled.getId().intValue()))))
                .andExpect(jsonPath("$.data", not(hasItem(expired.getId().intValue()))));
    }

    @Test
    void createLoginLog_应落库() throws Exception {
        mockMvc.perform(post("/rpc-api/system/login-log/create").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logType\":100,\"traceId\":\"t1\",\"userId\":7,\"userType\":1,\"username\":\"m\","
                                + "\"result\":0,\"userIp\":\"10.0.0.1\",\"userAgent\":\"JUnit\"}"))
                .andExpect(jsonPath("$.data").value(true));
        assertThat(loginLogMapper.selectList()).extracting(LoginLogDO::getTraceId).contains("t1");
    }

    // ========== 覆盖度 ==========

    @Test
    void 所有RpcApi实现类都覆盖了对应Feign接口的全部方法() throws Exception {
        List<Class<?>> apis = scanFeignApis();
        assertThat(apis).as("扫描到的 Feign 接口").hasSize(17);

        Set<String> pending = new TreeSet<>(PENDING_APIS.keySet());
        for (Class<?> api : apis) {
            String implName = api.getName() + "Impl";
            Class<?> impl;
            try {
                impl = Class.forName(implName);
            } catch (ClassNotFoundException e) {
                assertThat(PENDING_APIS).as("%s 缺少实现 %s", api.getSimpleName(), implName).containsKey(api.getSimpleName());
                continue;
            }
            assertThat(pending.remove(api.getSimpleName()))
                    .as("%s 已实现，应从 PENDING_APIS 中移除", api.getSimpleName()).isFalse();
            assertThat(api.isAssignableFrom(impl)).as("%s 应实现 %s", implName, api.getSimpleName()).isTrue();
            assertThat(impl.isAnnotationPresent(RestController.class)).as("%s 应为 @RestController", implName).isTrue();
            for (Method method : api.getMethods()) {
                if (method.isDefault() || Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                Method implMethod = impl.getMethod(method.getName(), method.getParameterTypes());
                assertThat(implMethod.getDeclaringClass()).as("%s#%s", implName, method.getName()).isEqualTo(impl);
            }
        }
    }

    private static List<Class<?>> scanFeignApis() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        CachingMetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resolver);
        List<Class<?>> result = new ArrayList<>();
        for (org.springframework.core.io.Resource resource :
                resolver.getResources("classpath*:com/ccloud/module/system/api/**/*Api.class")) {
            var metadata = readerFactory.getMetadataReader(resource).getAnnotationMetadata();
            if (metadata.isInterface() && metadata.hasAnnotation(FeignClient.class.getName())) {
                result.add(Class.forName(metadata.getClassName()));
            }
        }
        return result;
    }

    // ========== 数据准备 ==========

    private Long createUser(String username, Long deptId) {
        UserSaveReqVO reqVO = new UserSaveReqVO();
        reqVO.setUsername(username);
        reqVO.setNickname("昵称" + username);
        reqVO.setPassword("admin123");
        reqVO.setDeptId(deptId);
        return userService.createUser(reqVO);
    }

    private Long createDept(String name, Long parentId) {
        DeptSaveReqVO reqVO = new DeptSaveReqVO();
        reqVO.setName(name);
        reqVO.setParentId(parentId);
        reqVO.setSort(1);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return deptService.createDept(reqVO);
    }

    private TenantDO insertTenant(String name, CommonStatusEnum status, LocalDateTime expireTime) {
        TenantDO tenant = new TenantDO().setName(name).setContactName("联系人").setContactMobile("13800000000")
                .setStatus(status.getStatus()).setPackageId(99L).setExpireTime(expireTime).setAccountCount(10)
                .setWebsite("");
        tenantMapper.insert(tenant);
        return tenant;
    }
}
