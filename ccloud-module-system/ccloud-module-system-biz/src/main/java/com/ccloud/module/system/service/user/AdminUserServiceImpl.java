package com.ccloud.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.common.util.validation.ValidationUtils;
import com.ccloud.framework.datapermission.core.annotation.DataPermission;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.infra.api.config.ConfigApi;
import com.ccloud.module.infra.api.file.FileApi;
import com.ccloud.module.infra.api.file.dto.FileCreateReqDTO;
import com.ccloud.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import com.ccloud.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserImportExcelVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserImportRespVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserPageReqVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import com.ccloud.module.system.dal.dataobject.dept.UserPostDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.dal.mysql.dept.UserPostMapper;
import com.ccloud.module.system.dal.mysql.user.AdminUserMapper;
import com.ccloud.module.system.service.dept.DeptService;
import com.ccloud.module.system.service.dept.PostService;
import com.ccloud.module.system.service.oauth2.OAuth2TokenService;
import com.ccloud.module.system.service.permission.PermissionService;
import com.ccloud.module.system.service.tenant.TenantService;
import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.*;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private UserPostMapper userPostMapper;
    @Resource
    private DeptService deptService;
    @Resource
    private PostService postService;
    @Resource
    private PermissionService permissionService;
    @Resource
    @Lazy
    private TenantService tenantService;
    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private Validator validator;

    /**
     * 导入用户的初始密码，存于 infra 参数配置
     */
    static final String INIT_PASSWORD_CONFIG_KEY = "system.user.init-password";

    @Resource
    @Lazy
    private ConfigApi configApi;
    @Resource
    @Lazy
    private FileApi fileApi;

    // ========== 创建与修改 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserSaveReqVO createReqVO) {
        validateTenantAccountQuota();
        validateUserForCreateOrUpdate(null, createReqVO.getUsername(), createReqVO.getMobile(),
                createReqVO.getEmail(), createReqVO.getDeptId(), createReqVO.getPostIds());

        AdminUserDO user = BeanUtils.toBean(createReqVO, AdminUserDO.class);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        user.setPassword(encodePassword(createReqVO.getPassword()));
        userMapper.insert(user);

        updateUserPosts(user.getId(), createReqVO.getPostIds());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserSaveReqVO updateReqVO) {
        validateUserForCreateOrUpdate(updateReqVO.getId(), updateReqVO.getUsername(), updateReqVO.getMobile(),
                updateReqVO.getEmail(), updateReqVO.getDeptId(), updateReqVO.getPostIds());

        AdminUserDO updateObj = BeanUtils.toBean(updateReqVO, AdminUserDO.class);
        // 修改用户信息不允许顺带改密码，改密码有独立的接口与权限
        updateObj.setPassword(null);
        userMapper.updateById(updateObj);

        updateUserPosts(updateReqVO.getId(), updateReqVO.getPostIds());
    }

    /**
     * 按差量同步 system_user_post
     */
    private void updateUserPosts(Long userId, Collection<Long> postIds) {
        Set<Long> dbPostIds = CollectionUtils.convertSet(userPostMapper.selectListByUserId(userId), UserPostDO::getPostId);
        Set<Long> targetPostIds = postIds == null ? Collections.emptySet() : new LinkedHashSet<>(postIds);
        Collection<Long> createPostIds = CollUtil.subtract(targetPostIds, dbPostIds);
        Collection<Long> deletePostIds = CollUtil.subtract(dbPostIds, targetPostIds);
        if (CollUtil.isNotEmpty(createPostIds)) {
            userPostMapper.insertBatch(CollectionUtils.convertList(createPostIds,
                    postId -> new UserPostDO().setUserId(userId).setPostId(postId)));
        }
        if (CollUtil.isNotEmpty(deletePostIds)) {
            userPostMapper.deleteByUserIdAndPostIds(userId, deletePostIds);
        }
    }

    @Override
    public void updateUserLogin(Long id, String loginIp) {
        userMapper.updateById(new AdminUserDO().setId(id).setLoginIp(loginIp).setLoginDate(LocalDateTime.now()));
    }

    @Override
    public void updateUserProfile(Long id, UserProfileUpdateReqVO reqVO) {
        validateUserExists(id);
        validateMobileUnique(id, reqVO.getMobile());
        validateEmailUnique(id, reqVO.getEmail());
        userMapper.updateById(BeanUtils.toBean(reqVO, AdminUserDO.class).setId(id));
    }

    @Override
    public String updateUserAvatar(Long id, String name, byte[] content) {
        validateUserExists(id);
        FileCreateReqDTO reqDTO = new FileCreateReqDTO();
        reqDTO.setName(name);
        reqDTO.setContent(content);
        // 文件被拒绝（类型、大小）时 getCheckedData 抛出 infra 的错误码，头像保持不变
        String url = fileApi.createFile(reqDTO).getCheckedData();
        userMapper.updateById(new AdminUserDO().setId(id).setAvatar(url));
        return url;
    }

    /**
     * 本人修改密码也作废全部令牌（含当前会话）：改密码往往是怀疑密码泄露，其他设备上的会话必须下线
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserPassword(Long id, UserProfileUpdatePasswordReqVO reqVO) {
        AdminUserDO user = validateUserExists(id);
        if (!isPasswordMatch(reqVO.getOldPassword(), user.getPassword())) {
            throw exception(USER_PASSWORD_FAILED);
        }
        userMapper.updateById(new AdminUserDO().setId(id).setPassword(encodePassword(reqVO.getNewPassword())));
        removeUserTokens(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetUserPassword(Long id, String password) {
        validateUserExists(id);
        userMapper.updateById(new AdminUserDO().setId(id).setPassword(encodePassword(password)));
        removeUserTokens(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long id, Integer status) {
        validateUserExists(id);
        userMapper.updateById(new AdminUserDO().setId(id).setStatus(status));
        if (CommonStatusEnum.isDisable(status)) {
            removeUserTokens(id);
        }
    }

    /**
     * 作废用户的全部访问令牌与刷新令牌。网关缓存令牌校验结果，已缓存的令牌最多还能用一个缓存周期
     */
    private void removeUserTokens(Long id) {
        oauth2TokenService.removeAccessTokensByUser(id, UserTypeEnum.ADMIN.getValue());
    }

    // ========== 删除 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        validateUserExists(id);
        userMapper.deleteById(id);
        permissionService.processUserDeleted(id);
        userPostMapper.deleteByUserId(id);
        removeUserTokens(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserList(List<Long> ids) {
        ids.forEach(this::deleteUser);
    }

    // ========== 查询 ==========

    @Override
    public AdminUserDO getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public AdminUserDO getUserByMobile(String mobile) {
        return userMapper.selectByMobile(mobile);
    }

    @Override
    @DataPermission
    public PageResult<AdminUserDO> getUserPage(UserPageReqVO reqVO) {
        Set<Long> deptIds = reqVO.getDeptId() != null ? deptService.getSelfAndChildDeptIdList(reqVO.getDeptId()) : null;
        return userMapper.selectPage(reqVO, new LambdaQueryWrapperX<AdminUserDO>()
                .likeIfPresent(AdminUserDO::getUsername, reqVO.getUsername())
                .likeIfPresent(AdminUserDO::getMobile, reqVO.getMobile())
                .eqIfPresent(AdminUserDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AdminUserDO::getCreateTime, reqVO.getCreateTime())
                .inIfPresent(AdminUserDO::getDeptId, deptIds)
                .orderByDesc(AdminUserDO::getId));
    }

    @Override
    public AdminUserDO getUser(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public List<AdminUserDO> getUserList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return userMapper.selectByIds(ids);
    }

    @Override
    public List<AdminUserDO> getUserListByDeptIds(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyList();
        }
        return userMapper.selectList(AdminUserDO::getDeptId, deptIds);
    }

    @Override
    public List<AdminUserDO> getUserListByPostIds(Collection<Long> postIds) {
        if (CollUtil.isEmpty(postIds)) {
            return Collections.emptyList();
        }
        Set<Long> userIds = CollectionUtils.convertSet(userPostMapper.selectListByPostIds(postIds), UserPostDO::getUserId);
        return getUserList(userIds);
    }

    @Override
    public List<AdminUserDO> getUserListByStatus(Integer status) {
        return userMapper.selectList(AdminUserDO::getStatus, status);
    }

    @Override
    public void validateUserList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        Map<Long, AdminUserDO> userMap = CollectionUtils.convertMap(userMapper.selectByIds(ids), AdminUserDO::getId);
        for (Long id : ids) {
            AdminUserDO user = userMap.get(id);
            if (user == null) {
                throw exception(USER_NOT_EXISTS);
            }
            if (!CommonStatusEnum.isEnable(user.getStatus())) {
                throw exception(USER_IS_DISABLE, user.getNickname());
            }
        }
    }

    // ========== 导入 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserImportRespVO importUserList(List<UserImportExcelVO> importUsers, boolean isUpdateSupport) {
        if (CollUtil.isEmpty(importUsers)) {
            throw exception(USER_IMPORT_LIST_IS_EMPTY);
        }
        String initPassword = getInitPassword();
        UserImportRespVO respVO = new UserImportRespVO();
        for (UserImportExcelVO importUser : importUsers) {
            String username = StrUtil.nullToEmpty(importUser.getUsername());
            try {
                importUser(importUser, isUpdateSupport, initPassword, respVO);
            } catch (ConstraintViolationException ex) {
                respVO.getFailureUsernames().put(username, ex.getConstraintViolations().iterator().next().getMessage());
            } catch (ServiceException ex) {
                respVO.getFailureUsernames().put(username, ex.getMessage());
            }
        }
        return respVO;
    }

    private void importUser(UserImportExcelVO importUser, boolean isUpdateSupport, String initPassword,
                            UserImportRespVO respVO) {
        ValidationUtils.validate(validator, importUser);
        AdminUserDO existUser = userMapper.selectByUsername(importUser.getUsername());
        if (existUser == null) {
            validateTenantAccountQuota();
            validateUserForCreateOrUpdate(null, importUser.getUsername(), importUser.getMobile(),
                    importUser.getEmail(), importUser.getDeptId(), null);
            AdminUserDO user = BeanUtils.toBean(importUser, AdminUserDO.class);
            if (user.getStatus() == null) {
                user.setStatus(CommonStatusEnum.ENABLE.getStatus());
            }
            user.setPassword(encodePassword(initPassword));
            userMapper.insert(user);
            respVO.getCreateUsernames().add(importUser.getUsername());
            return;
        }
        if (!isUpdateSupport) {
            throw exception(USER_USERNAME_EXISTS);
        }
        validateUserForCreateOrUpdate(existUser.getId(), importUser.getUsername(), importUser.getMobile(),
                importUser.getEmail(), importUser.getDeptId(), null);
        AdminUserDO updateObj = BeanUtils.toBean(importUser, AdminUserDO.class);
        updateObj.setId(existUser.getId());
        userMapper.updateById(updateObj);
        respVO.getUpdateUsernames().add(importUser.getUsername());
    }

    /**
     * 取不到值或 infra 不可用都拒绝导入：没有初始密码时生成的账号无法登录，也不能退回到某个硬编码的弱密码
     */
    private String getInitPassword() {
        String password;
        try {
            password = configApi.getConfigValueByKey(INIT_PASSWORD_CONFIG_KEY).getCheckedData();
        } catch (RuntimeException e) {
            log.warn("[getInitPassword][读取参数 {} 失败]", INIT_PASSWORD_CONFIG_KEY, e);
            throw exception(USER_IMPORT_INIT_PASSWORD);
        }
        if (StrUtil.isEmpty(password)) {
            throw exception(USER_IMPORT_INIT_PASSWORD);
        }
        return password;
    }

    // ========== 校验 ==========

    private void validateTenantAccountQuota() {
        tenantService.handleTenantInfo(tenant -> {
            long count = userMapper.selectCount();
            if (count >= tenant.getAccountCount()) {
                throw exception(USER_COUNT_MAX, tenant.getAccountCount());
            }
        });
    }

    private void validateUserForCreateOrUpdate(Long id, String username, String mobile, String email,
                                               Long deptId, Collection<Long> postIds) {
        if (id != null) {
            validateUserExists(id);
        }
        validateUsernameUnique(id, username);
        validateMobileUnique(id, mobile);
        validateEmailUnique(id, email);
        if (deptId != null) {
            deptService.validateDeptList(Collections.singleton(deptId));
        }
        postService.validatePostList(postIds);
    }

    private AdminUserDO validateUserExists(Long id) {
        AdminUserDO user = id != null ? userMapper.selectById(id) : null;
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        return user;
    }

    private void validateUsernameUnique(Long id, String username) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        AdminUserDO user = userMapper.selectByUsername(username);
        if (user != null && !user.getId().equals(id)) {
            throw exception(USER_USERNAME_EXISTS);
        }
    }

    /**
     * 手机号为空时不参与唯一性校验
     */
    private void validateMobileUnique(Long id, String mobile) {
        if (StrUtil.isBlank(mobile)) {
            return;
        }
        AdminUserDO user = userMapper.selectByMobile(mobile);
        if (user != null && !user.getId().equals(id)) {
            throw exception(USER_MOBILE_EXISTS);
        }
    }

    /**
     * 邮箱为空时不参与唯一性校验
     */
    private void validateEmailUnique(Long id, String email) {
        if (StrUtil.isBlank(email)) {
            return;
        }
        AdminUserDO user = userMapper.selectByEmail(email);
        if (user != null && !user.getId().equals(id)) {
            throw exception(USER_EMAIL_EXISTS);
        }
    }

    @Override
    public boolean isPasswordMatch(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }
}
