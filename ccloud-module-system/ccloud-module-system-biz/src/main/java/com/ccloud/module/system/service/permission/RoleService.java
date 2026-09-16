package com.ccloud.module.system.service.permission;

import com.ccloud.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import com.ccloud.module.system.dal.dataobject.permission.RoleDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 角色 Service
 */
public interface RoleService {

    /**
     * @param type 角色类型，为空时为自定义角色
     * @return 角色编号
     */
    Long createRole(@Valid RoleSaveReqVO createReqVO, Integer type);

    RoleDO getRole(Long id);

    /**
     * @return 当前租户的全部角色
     */
    List<RoleDO> getRoleList();
}
