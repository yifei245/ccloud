package com.ccloud.module.system.service.permission;

import cn.hutool.core.util.ObjectUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import com.ccloud.module.system.dal.dataobject.permission.RoleDO;
import com.ccloud.module.system.dal.mysql.permission.RoleMapper;
import com.ccloud.module.system.enums.permission.DataScopeEnum;
import com.ccloud.module.system.enums.permission.RoleCodeEnum;
import com.ccloud.module.system.enums.permission.RoleTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class RoleServiceImpl implements RoleService {

    @Resource
    private RoleMapper roleMapper;

    @Override
    public Long createRole(RoleSaveReqVO createReqVO, Integer type) {
        validateRoleDuplicate(createReqVO.getName(), createReqVO.getCode(), null);

        RoleDO role = BeanUtils.toBean(createReqVO, RoleDO.class)
                .setType(ObjectUtil.defaultIfNull(type, RoleTypeEnum.CUSTOM.getType()))
                .setStatus(ObjectUtil.defaultIfNull(createReqVO.getStatus(), CommonStatusEnum.ENABLE.getStatus()))
                .setDataScope(DataScopeEnum.ALL.getScope());
        roleMapper.insert(role);
        return role.getId();
    }

    /**
     * 名称、标识在租户内唯一；super_admin 标识保留给系统内置的超级管理员
     */
    void validateRoleDuplicate(String name, String code, Long id) {
        if (RoleCodeEnum.SUPER_ADMIN.getCode().equals(code)) {
            throw exception(ROLE_ADMIN_CODE_ERROR, code);
        }
        RoleDO role = roleMapper.selectByName(name);
        if (role != null && !role.getId().equals(id)) {
            throw exception(ROLE_NAME_DUPLICATE, name);
        }
        role = roleMapper.selectByCode(code);
        if (role != null && !role.getId().equals(id)) {
            throw exception(ROLE_CODE_DUPLICATE, code);
        }
    }

    @Override
    public RoleDO getRole(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public List<RoleDO> getRoleList() {
        return roleMapper.selectList();
    }
}
