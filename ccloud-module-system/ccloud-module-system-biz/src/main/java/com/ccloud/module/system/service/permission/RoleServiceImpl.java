package com.ccloud.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.security.core.service.PermissionCacheRefresher;
import com.ccloud.module.system.controller.admin.permission.vo.role.RolePageReqVO;
import com.ccloud.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import com.ccloud.module.system.dal.dataobject.permission.RoleDO;
import com.ccloud.module.system.dal.mysql.permission.RoleMapper;
import com.ccloud.module.system.enums.permission.DataScopeEnum;
import com.ccloud.module.system.enums.permission.RoleCodeEnum;
import com.ccloud.module.system.enums.permission.RoleTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.dal.redis.RedisKeyConstants.ROLE;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class RoleServiceImpl implements RoleService {

    @Resource
    private RoleMapper roleMapper;
    @Resource
    @Lazy
    private PermissionService permissionService;
    /**
     * 自身代理：带缓存注解的方法在类内调用时必须经过代理才生效
     */
    @Resource
    @Lazy
    private RoleService self;
    @Resource
    private ObjectProvider<PermissionCacheRefresher> permissionCacheRefresherProvider;

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

    @Override
    @CacheEvict(value = ROLE, key = "#updateReqVO.id")
    public void updateRole(RoleSaveReqVO updateReqVO) {
        validateRoleForUpdate(updateReqVO.getId());
        validateRoleDuplicate(updateReqVO.getName(), updateReqVO.getCode(), updateReqVO.getId());

        roleMapper.updateById(BeanUtils.toBean(updateReqVO, RoleDO.class));
        refreshPermissionCache();
    }

    @Override
    @CacheEvict(value = ROLE, key = "#id")
    public void updateRoleStatus(Long id, Integer status) {
        validateRoleForUpdate(id);
        roleMapper.updateById(new RoleDO().setId(id).setStatus(status));
        refreshPermissionCache();
    }

    @Override
    @CacheEvict(value = ROLE, key = "#id")
    public void updateRoleDataScope(Long id, Integer dataScope, Set<Long> dataScopeDeptIds) {
        validateRoleForUpdate(id);
        Set<Long> deptIds = DataScopeEnum.DEPT_CUSTOM.getScope().equals(dataScope) && dataScopeDeptIds != null
                ? dataScopeDeptIds : Collections.emptySet();
        roleMapper.updateById(new RoleDO().setId(id).setDataScope(dataScope).setDataScopeDeptIds(deptIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = ROLE, key = "#id")
    public void deleteRole(Long id) {
        validateRoleForUpdate(id);
        roleMapper.deleteById(id);
        permissionService.processRoleDeleted(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = ROLE, allEntries = true)
    public void deleteRoleList(List<Long> ids) {
        ids.forEach(this::validateRoleForUpdate);
        roleMapper.deleteByIds(ids);
        ids.forEach(permissionService::processRoleDeleted);
    }

    private void refreshPermissionCache() {
        permissionCacheRefresherProvider.ifAvailable(refresher -> refresher.publish(null));
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

    /**
     * 角色存在，且不是内置角色（超级管理员、租户管理员等由系统维护）
     */
    private RoleDO validateRoleForUpdate(Long id) {
        RoleDO role = roleMapper.selectById(id);
        if (role == null) {
            throw exception(ROLE_NOT_EXISTS);
        }
        if (RoleTypeEnum.SYSTEM.getType().equals(role.getType())) {
            throw exception(ROLE_CAN_NOT_UPDATE_SYSTEM_TYPE_ROLE);
        }
        return role;
    }

    @Override
    public RoleDO getRole(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    @Cacheable(value = ROLE, key = "#id", unless = "#result == null")
    public RoleDO getRoleFromCache(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public List<RoleDO> getRoleList() {
        return roleMapper.selectList();
    }

    @Override
    public List<RoleDO> getRoleList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return roleMapper.selectByIds(ids);
    }

    @Override
    public List<RoleDO> getRoleListFromCache(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<RoleDO> roles = new ArrayList<>(ids.size());
        ids.forEach(id -> CollectionUtils.addIfNotNull(roles, self.getRoleFromCache(id)));
        return roles;
    }

    @Override
    public List<RoleDO> getRoleListByStatus(Collection<Integer> statuses) {
        return roleMapper.selectListByStatus(statuses);
    }

    @Override
    public PageResult<RoleDO> getRolePage(RolePageReqVO reqVO) {
        return roleMapper.selectPage(reqVO);
    }

    @Override
    public boolean hasAnySuperAdmin(Collection<Long> ids) {
        return CollectionUtils.anyMatch(getRoleListFromCache(ids),
                role -> RoleCodeEnum.SUPER_ADMIN.getCode().equals(role.getCode()));
    }

    @Override
    public void validateRoleList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        Map<Long, RoleDO> roleMap = CollectionUtils.convertMap(roleMapper.selectByIds(ids), RoleDO::getId);
        for (Long id : ids) {
            RoleDO role = roleMap.get(id);
            if (role == null) {
                throw exception(ROLE_NOT_EXISTS);
            }
            if (!CommonStatusEnum.isEnable(role.getStatus())) {
                throw exception(ROLE_IS_DISABLE, role.getName());
            }
        }
    }
}
