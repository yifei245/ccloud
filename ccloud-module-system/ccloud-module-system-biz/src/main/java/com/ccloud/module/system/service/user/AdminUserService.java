package com.ccloud.module.system.service.user;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import com.ccloud.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserImportExcelVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserImportRespVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserPageReqVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * 管理后台用户 Service
 */
public interface AdminUserService {

    /**
     * 创建用户：校验租户账号配额、账号/手机/邮箱唯一性、部门与岗位可用，密码以 BCrypt 存储
     *
     * @return 用户编号
     */
    Long createUser(@Valid UserSaveReqVO createReqVO);

    /**
     * 修改用户基本信息与岗位；不修改密码
     */
    void updateUser(@Valid UserSaveReqVO updateReqVO);

    /**
     * 记录最后登录 IP 与时间
     */
    void updateUserLogin(Long id, String loginIp);

    void updateUserProfile(Long id, @Valid UserProfileUpdateReqVO reqVO);

    /**
     * 本人修改密码，需校验旧密码
     */
    void updateUserPassword(Long id, @Valid UserProfileUpdatePasswordReqVO reqVO);

    /**
     * 管理员重置密码，不校验旧密码
     */
    void resetUserPassword(Long id, String password);

    void updateUserStatus(Long id, Integer status);

    /**
     * 删除用户，并清理角色、岗位关联
     */
    void deleteUser(Long id);

    void deleteUserList(List<Long> ids);

    AdminUserDO getUserByUsername(String username);

    AdminUserDO getUserByMobile(String mobile);

    /**
     * 分页查询，受部门数据权限约束
     */
    PageResult<AdminUserDO> getUserPage(UserPageReqVO reqVO);

    AdminUserDO getUser(Long id);

    List<AdminUserDO> getUserList(Collection<Long> ids);

    List<AdminUserDO> getUserListByDeptIds(Collection<Long> deptIds);

    List<AdminUserDO> getUserListByPostIds(Collection<Long> postIds);

    List<AdminUserDO> getUserListByStatus(Integer status);

    /**
     * 校验用户均存在且开启
     */
    void validateUserList(Collection<Long> ids);

    /**
     * 批量导入：逐行处理，单行失败不影响其他行
     *
     * @param isUpdateSupport 账号已存在时是否更新
     */
    UserImportRespVO importUserList(List<UserImportExcelVO> importUsers, boolean isUpdateSupport);

    boolean isPasswordMatch(String rawPassword, String encodedPassword);
}
