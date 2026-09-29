package com.ccloud.module.system.service.user;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import com.ccloud.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserImportExcelVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserImportRespVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserPageReqVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import com.ccloud.module.system.dal.dataobject.dept.DeptDO;
import com.ccloud.module.system.dal.dataobject.dept.PostDO;
import com.ccloud.module.system.dal.dataobject.tenant.TenantDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.dal.mysql.dept.DeptMapper;
import com.ccloud.module.system.dal.mysql.dept.PostMapper;
import com.ccloud.module.system.dal.mysql.dept.UserPostMapper;
import com.ccloud.module.system.dal.mysql.user.AdminUserMapper;
import com.ccloud.module.system.service.dept.DeptServiceImpl;
import com.ccloud.module.system.service.dept.PostServiceImpl;
import com.ccloud.module.system.service.oauth2.OAuth2TokenService;
import com.ccloud.module.system.service.permission.PermissionService;
import com.ccloud.module.system.service.permission.PermissionServiceImpl;
import com.ccloud.module.system.service.tenant.TenantInfoHandler;
import com.ccloud.module.system.service.tenant.TenantService;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Set;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Import({AdminUserServiceImpl.class, DeptServiceImpl.class, PostServiceImpl.class, PermissionServiceImpl.class,
        LocalValidatorFactoryBean.class})
@TestPropertySource(properties = "ccloud.system.user.init-password=init123")
class AdminUserServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private AdminUserServiceImpl userService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private PostMapper postMapper;
    @Resource
    private UserPostMapper userPostMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private TenantService tenantService;
    @MockitoBean
    private OAuth2TokenService oauth2TokenService;

    // ========== 创建 ==========

    @Test
    void createUser_成功时密码加密存储_默认开启_并写入岗位关联() {
        DeptDO dept = insertDept("研发部", 0L, CommonStatusEnum.ENABLE);
        PostDO post1 = insertPost("dev", "开发", CommonStatusEnum.ENABLE);
        PostDO post2 = insertPost("qa", "测试", CommonStatusEnum.ENABLE);
        UserSaveReqVO reqVO = newReqVO("zhangsan", "13700000001", "zs@ccloud.local");
        reqVO.setDeptId(dept.getId());
        reqVO.setPostIds(Set.of(post1.getId(), post2.getId()));

        Long id = userService.createUser(reqVO);

        AdminUserDO user = userMapper.selectById(id);
        assertThat(user.getUsername()).isEqualTo("zhangsan");
        assertThat(passwordEncoder.matches("pass123", user.getPassword())).isTrue();
        assertThat(user.getStatus()).isEqualTo(CommonStatusEnum.ENABLE.getStatus());
        assertThat(user.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(user.getPostIds()).containsExactlyInAnyOrder(post1.getId(), post2.getId());
        assertThat(userPostMapper.selectListByUserId(id)).hasSize(2);
    }

    @Test
    void createUser_账号重复() {
        userService.createUser(newReqVO("dup", null, null));
        assertServiceException(() -> userService.createUser(newReqVO("dup", null, null)), USER_USERNAME_EXISTS);
    }

    @Test
    void createUser_手机号重复() {
        userService.createUser(newReqVO("a1", "13700000002", null));
        assertServiceException(() -> userService.createUser(newReqVO("a2", "13700000002", null)), USER_MOBILE_EXISTS);
    }

    @Test
    void createUser_邮箱重复() {
        userService.createUser(newReqVO("b1", null, "b@ccloud.local"));
        assertServiceException(() -> userService.createUser(newReqVO("b2", null, "b@ccloud.local")), USER_EMAIL_EXISTS);
    }

    @Test
    void createUser_部门停用() {
        DeptDO dept = insertDept("停用部", 0L, CommonStatusEnum.DISABLE);
        UserSaveReqVO reqVO = newReqVO("d1", null, null);
        reqVO.setDeptId(dept.getId());
        assertServiceException(() -> userService.createUser(reqVO), DEPT_NOT_ENABLE);
    }

    @Test
    void createUser_部门不存在() {
        UserSaveReqVO reqVO = newReqVO("d2", null, null);
        reqVO.setDeptId(-1L);
        assertServiceException(() -> userService.createUser(reqVO), DEPT_NOT_FOUND);
    }

    @Test
    void createUser_岗位停用() {
        PostDO post = insertPost("old", "停用岗", CommonStatusEnum.DISABLE);
        UserSaveReqVO reqVO = newReqVO("p1", null, null);
        reqVO.setPostIds(Set.of(post.getId()));
        assertServiceException(() -> userService.createUser(reqVO), POST_NOT_ENABLE);
    }

    @Test
    void createUser_空手机号与空邮箱不参与唯一性校验() {
        userService.createUser(newReqVO("c1", null, null));
        userService.createUser(newReqVO("c2", "", ""));
        assertThat(userMapper.selectCount()).isEqualTo(2);
    }

    @Test
    void createUser_超过租户账号配额() {
        doAnswer(inv -> {
            inv.<TenantInfoHandler>getArgument(0).handle(new TenantDO().setAccountCount(1));
            return null;
        }).when(tenantService).handleTenantInfo(any());
        userService.createUser(newReqVO("q1", null, null));

        assertServiceException(() -> userService.createUser(newReqVO("q2", null, null)), USER_COUNT_MAX);
    }

    // ========== 修改 ==========

    @Test
    void updateUser_不修改密码_同步岗位关联() {
        PostDO post1 = insertPost("p1", "岗一", CommonStatusEnum.ENABLE);
        PostDO post2 = insertPost("p2", "岗二", CommonStatusEnum.ENABLE);
        UserSaveReqVO createReqVO = newReqVO("upd", null, null);
        createReqVO.setPostIds(Set.of(post1.getId()));
        Long id = userService.createUser(createReqVO);
        String encodedPassword = userMapper.selectById(id).getPassword();

        UserSaveReqVO reqVO = newReqVO("upd", "13700000009", null);
        reqVO.setId(id);
        reqVO.setNickname("新昵称");
        reqVO.setPassword("ignored");
        reqVO.setPostIds(Set.of(post2.getId()));
        userService.updateUser(reqVO);

        AdminUserDO user = userMapper.selectById(id);
        assertThat(user.getNickname()).isEqualTo("新昵称");
        assertThat(user.getMobile()).isEqualTo("13700000009");
        assertThat(user.getPassword()).isEqualTo(encodedPassword);
        assertThat(userPostMapper.selectListByUserId(id)).extracting("postId").containsExactly(post2.getId());
    }

    @Test
    void updateUser_不存在() {
        UserSaveReqVO reqVO = newReqVO("nobody", null, null);
        reqVO.setId(-1L);
        assertServiceException(() -> userService.updateUser(reqVO), USER_NOT_EXISTS);
    }

    @Test
    void updateUser_手机号与他人重复_与自己相同则允许() {
        Long other = userService.createUser(newReqVO("other", "13700000010", null));
        Long self = userService.createUser(newReqVO("self", "13700000011", null));

        UserSaveReqVO same = newReqVO("self", "13700000011", null);
        same.setId(self);
        userService.updateUser(same);

        UserSaveReqVO conflict = newReqVO("self", "13700000010", null);
        conflict.setId(self);
        assertServiceException(() -> userService.updateUser(conflict), USER_MOBILE_EXISTS);
        assertThat(other).isNotNull();
    }

    @Test
    void updateUserLogin_记录登录IP与时间() {
        Long id = userService.createUser(newReqVO("login", null, null));
        userService.updateUserLogin(id, "10.0.0.1");
        AdminUserDO user = userMapper.selectById(id);
        assertThat(user.getLoginIp()).isEqualTo("10.0.0.1");
        assertThat(user.getLoginDate()).isNotNull();
    }

    @Test
    void updateUserProfile_只修改个人资料字段() {
        Long id = userService.createUser(newReqVO("profile", null, null));
        UserProfileUpdateReqVO reqVO = new UserProfileUpdateReqVO();
        reqVO.setNickname("个人");
        reqVO.setEmail("me@ccloud.local");
        reqVO.setSex(2);
        userService.updateUserProfile(id, reqVO);

        AdminUserDO user = userMapper.selectById(id);
        assertThat(user.getNickname()).isEqualTo("个人");
        assertThat(user.getEmail()).isEqualTo("me@ccloud.local");
        assertThat(user.getSex()).isEqualTo(2);
        assertThat(user.getUsername()).isEqualTo("profile");
    }

    @Test
    void updateUserPassword_旧密码正确时修改成功() {
        Long id = userService.createUser(newReqVO("pwd", null, null));
        UserProfileUpdatePasswordReqVO reqVO = new UserProfileUpdatePasswordReqVO();
        reqVO.setOldPassword("pass123");
        reqVO.setNewPassword("newpass");
        userService.updateUserPassword(id, reqVO);

        AdminUserDO user = userMapper.selectById(id);
        assertThat(userService.isPasswordMatch("newpass", user.getPassword())).isTrue();
        assertThat(userService.isPasswordMatch("pass123", user.getPassword())).isFalse();
        // 本人改密码也下线全部会话（含当前会话），需重新登录
        verify(oauth2TokenService).removeAccessTokensByUser(id, UserTypeEnum.ADMIN.getValue());
    }

    @Test
    void updateUserPassword_旧密码错误() {
        Long id = userService.createUser(newReqVO("pwd2", null, null));
        UserProfileUpdatePasswordReqVO reqVO = new UserProfileUpdatePasswordReqVO();
        reqVO.setOldPassword("wrong");
        reqVO.setNewPassword("newpass");
        assertServiceException(() -> userService.updateUserPassword(id, reqVO), USER_PASSWORD_FAILED);
        verify(oauth2TokenService, never()).removeAccessTokensByUser(anyLong(), anyInt());
    }

    @Test
    void resetUserPassword_重置后新密码可校验通过() {
        Long id = userService.createUser(newReqVO("reset", null, null));
        userService.resetUserPassword(id, "reset456");
        assertThat(passwordEncoder.matches("reset456", userMapper.selectById(id).getPassword())).isTrue();
        verify(oauth2TokenService).removeAccessTokensByUser(id, UserTypeEnum.ADMIN.getValue());
    }

    @Test
    void updateUserStatus() {
        Long id = userService.createUser(newReqVO("status", null, null));
        userService.updateUserStatus(id, CommonStatusEnum.DISABLE.getStatus());
        assertThat(userMapper.selectById(id).getStatus()).isEqualTo(CommonStatusEnum.DISABLE.getStatus());
        verify(oauth2TokenService).removeAccessTokensByUser(id, UserTypeEnum.ADMIN.getValue());
        assertServiceException(() -> userService.updateUserStatus(-1L, 0), USER_NOT_EXISTS);
    }

    @Test
    void updateUserStatus_启用时不作废令牌() {
        Long id = userService.createUser(newReqVO("enable", null, null));
        userService.updateUserStatus(id, CommonStatusEnum.ENABLE.getStatus());
        verify(oauth2TokenService, never()).removeAccessTokensByUser(anyLong(), anyInt());
    }

    // ========== 删除 ==========

    @Test
    void deleteUser_应清空角色与岗位关联() {
        PostDO post = insertPost("del", "岗", CommonStatusEnum.ENABLE);
        UserSaveReqVO reqVO = newReqVO("todelete", null, null);
        reqVO.setPostIds(Set.of(post.getId()));
        Long id = userService.createUser(reqVO);
        permissionService.assignUserRole(id, Set.of(1L, 2L));

        userService.deleteUser(id);

        assertThat(userMapper.selectById(id)).isNull();
        assertThat(permissionService.getUserRoleIdListByUserId(id)).isEmpty();
        assertThat(userPostMapper.selectListByUserId(id)).isEmpty();
        verify(oauth2TokenService).removeAccessTokensByUser(id, UserTypeEnum.ADMIN.getValue());
    }

    @Test
    void deleteUser_不存在() {
        assertServiceException(() -> userService.deleteUser(-1L), USER_NOT_EXISTS);
    }

    @Test
    void deleteUserList() {
        Long a = userService.createUser(newReqVO("dela", null, null));
        Long b = userService.createUser(newReqVO("delb", null, null));
        userService.deleteUserList(List.of(a, b));
        assertThat(userMapper.selectCount()).isZero();
        verify(oauth2TokenService).removeAccessTokensByUser(a, UserTypeEnum.ADMIN.getValue());
        verify(oauth2TokenService).removeAccessTokensByUser(b, UserTypeEnum.ADMIN.getValue());
    }

    // ========== 查询 ==========

    @Test
    void getUserPage_按部门过滤时包含子部门() {
        DeptDO root = insertDept("总部", 0L, CommonStatusEnum.ENABLE);
        DeptDO child = insertDept("分部", root.getId(), CommonStatusEnum.ENABLE);
        DeptDO grandChild = insertDept("小组", child.getId(), CommonStatusEnum.ENABLE);
        DeptDO other = insertDept("其他", 0L, CommonStatusEnum.ENABLE);
        createUserInDept("u1", root);
        createUserInDept("u2", grandChild);
        createUserInDept("u3", other);

        UserPageReqVO reqVO = new UserPageReqVO();
        reqVO.setDeptId(child.getId());
        assertThat(userService.getUserPage(reqVO).getList()).extracting(AdminUserDO::getUsername).containsExactly("u2");

        reqVO.setDeptId(root.getId());
        assertThat(userService.getUserPage(reqVO).getList()).extracting(AdminUserDO::getUsername)
                .containsExactlyInAnyOrder("u1", "u2");
    }

    @Test
    void getUserPage_按账号手机状态过滤() {
        userService.createUser(newReqVO("alice", "13700000021", null));
        Long bob = userService.createUser(newReqVO("alicebob", "13700000022", null));
        userService.updateUserStatus(bob, CommonStatusEnum.DISABLE.getStatus());
        userService.createUser(newReqVO("carol", "13700000023", null));

        UserPageReqVO reqVO = new UserPageReqVO();
        reqVO.setUsername("alice");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        PageResult<AdminUserDO> page = userService.getUserPage(reqVO);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getList()).extracting(AdminUserDO::getUsername).containsExactly("alice");

        reqVO = new UserPageReqVO();
        reqVO.setMobile("0023");
        assertThat(userService.getUserPage(reqVO).getList()).extracting(AdminUserDO::getUsername).containsExactly("carol");
    }

    @Test
    void getUser系列查询() {
        DeptDO dept = insertDept("查询部", 0L, CommonStatusEnum.ENABLE);
        PostDO post = insertPost("q", "查询岗", CommonStatusEnum.ENABLE);
        UserSaveReqVO reqVO = newReqVO("query", "13700000031", null);
        reqVO.setDeptId(dept.getId());
        reqVO.setPostIds(Set.of(post.getId()));
        Long id = userService.createUser(reqVO);
        Long another = userService.createUser(newReqVO("another", null, null));

        assertThat(userService.getUser(id).getUsername()).isEqualTo("query");
        assertThat(userService.getUserByUsername("query").getId()).isEqualTo(id);
        assertThat(userService.getUserByMobile("13700000031").getId()).isEqualTo(id);
        assertThat(userService.getUserList(List.of(id, another))).hasSize(2);
        assertThat(userService.getUserListByDeptIds(List.of(dept.getId()))).extracting(AdminUserDO::getId).containsExactly(id);
        assertThat(userService.getUserListByPostIds(List.of(post.getId()))).extracting(AdminUserDO::getId).containsExactly(id);
        assertThat(userService.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus())).hasSize(2);
    }

    @Test
    void validateUserList_不存在与已禁用() {
        Long ok = userService.createUser(newReqVO("valid", null, null));
        Long disabled = userService.createUser(newReqVO("invalid", null, null));
        userService.updateUserStatus(disabled, CommonStatusEnum.DISABLE.getStatus());

        userService.validateUserList(List.of(ok));
        assertServiceException(() -> userService.validateUserList(List.of(ok, -1L)), USER_NOT_EXISTS);
        assertServiceException(() -> userService.validateUserList(List.of(disabled)), USER_IS_DISABLE);
    }

    // ========== 导入 ==========

    @Test
    void importUserList_空列表() {
        assertServiceException(() -> userService.importUserList(List.of(), false), USER_IMPORT_LIST_IS_EMPTY);
    }

    @Test
    void importUserList_逐行返回新增_更新_失败明细() {
        userService.createUser(newReqVO("exists", null, null));
        DeptDO disabledDept = insertDept("停用部", 0L, CommonStatusEnum.DISABLE);

        List<UserImportExcelVO> rows = List.of(
                importRow("newone", "13700000041"),
                importRow("exists", "13700000042"),
                importRow("x", null),                                  // 账号格式不合法
                importRow("baddept", null).setDeptId(disabledDept.getId()));

        UserImportRespVO resp = userService.importUserList(rows, false);
        assertThat(resp.getCreateUsernames()).containsExactly("newone");
        assertThat(resp.getUpdateUsernames()).isEmpty();
        assertThat(resp.getFailureUsernames()).containsOnlyKeys("exists", "x", "baddept");
        assertThat(resp.getFailureUsernames().get("exists")).isEqualTo(USER_USERNAME_EXISTS.getMsg());
        // 新导入的账号使用配置的初始密码
        assertThat(passwordEncoder.matches("init123", userMapper.selectByUsername("newone").getPassword())).isTrue();

        UserImportRespVO resp2 = userService.importUserList(List.of(importRow("exists", "13700000042")), true);
        assertThat(resp2.getUpdateUsernames()).containsExactly("exists");
        assertThat(userMapper.selectByUsername("exists").getMobile()).isEqualTo("13700000042");
    }

    // ========== 工具 ==========

    private void createUserInDept(String username, DeptDO dept) {
        UserSaveReqVO reqVO = newReqVO(username, null, null);
        reqVO.setDeptId(dept.getId());
        userService.createUser(reqVO);
    }

    private DeptDO insertDept(String name, Long parentId, CommonStatusEnum status) {
        DeptDO dept = new DeptDO().setName(name).setParentId(parentId).setSort(0).setStatus(status.getStatus());
        deptMapper.insert(dept);
        return dept;
    }

    private PostDO insertPost(String code, String name, CommonStatusEnum status) {
        PostDO post = new PostDO().setCode(code).setName(name).setSort(0).setStatus(status.getStatus());
        postMapper.insert(post);
        return post;
    }

    private static UserImportExcelVO importRow(String username, String mobile) {
        return new UserImportExcelVO().setUsername(username).setNickname("导入").setMobile(mobile)
                .setSex(1).setStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    private static UserSaveReqVO newReqVO(String username, String mobile, String email) {
        UserSaveReqVO reqVO = new UserSaveReqVO();
        reqVO.setUsername(username);
        reqVO.setNickname("昵称");
        reqVO.setPassword("pass123");
        reqVO.setMobile(mobile);
        reqVO.setEmail(email);
        return reqVO;
    }
}
