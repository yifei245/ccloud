package com.ccloud.module.system.service.permission;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.permission.vo.role.RolePageReqVO;
import com.ccloud.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import com.ccloud.module.system.dal.dataobject.permission.RoleDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 角色 Service
 */
public interface RoleService {

    /**
     * @param type 角色类型，为空时为自定义角色
     * @return 角色编号
     */
    Long createRole(@Valid RoleSaveReqVO createReqVO, Integer type);

    /**
     * 内置角色不允许修改
     */
    void updateRole(@Valid RoleSaveReqVO updateReqVO);

    void updateRoleStatus(Long id, Integer status);

    /**
     * @param dataScopeDeptIds 仅数据范围为"指定部门"时保存
     */
    void updateRoleDataScope(Long id, Integer dataScope, Set<Long> dataScopeDeptIds);

    /**
     * 删除角色并清理用户、菜单关联；内置角色不允许删除
     */
    void deleteRole(Long id);

    void deleteRoleList(List<Long> ids);

    RoleDO getRole(Long id);

    RoleDO getRoleFromCache(Long id);

    /**
     * @return 当前租户的全部角色
     */
    List<RoleDO> getRoleList();

    List<RoleDO> getRoleList(Collection<Long> ids);

    List<RoleDO> getRoleListFromCache(Collection<Long> ids);

    List<RoleDO> getRoleListByStatus(Collection<Integer> statuses);

    PageResult<RoleDO> getRolePage(RolePageReqVO reqVO);

    boolean hasAnySuperAdmin(Collection<Long> ids);

    /**
     * 校验角色均存在且开启
     */
    void validateRoleList(Collection<Long> ids);
}
