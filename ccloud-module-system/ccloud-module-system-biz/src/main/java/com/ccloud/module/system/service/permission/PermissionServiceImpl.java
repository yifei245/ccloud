package com.ccloud.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.module.system.dal.dataobject.permission.RoleMenuDO;
import com.ccloud.module.system.dal.dataobject.permission.UserRoleDO;
import com.ccloud.module.system.dal.mysql.permission.RoleMenuMapper;
import com.ccloud.module.system.dal.mysql.permission.UserRoleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private UserRoleMapper userRoleMapper;

    /**
     * 差量更新而非"全删再插"：未变化的记录保持原样，审计字段与逻辑删除历史不会被无谓刷新
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoleMenu(Long roleId, Collection<Long> menuIds) {
        Set<Long> dbMenuIds = getRoleMenuListByRoleId(roleId);
        Set<Long> targetMenuIds = menuIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(menuIds);

        Collection<Long> createMenuIds = CollUtil.subtract(targetMenuIds, dbMenuIds);
        Collection<Long> deleteMenuIds = CollUtil.subtract(dbMenuIds, targetMenuIds);
        if (CollUtil.isNotEmpty(createMenuIds)) {
            roleMenuMapper.insertBatch(CollectionUtils.convertList(createMenuIds,
                    menuId -> new RoleMenuDO().setRoleId(roleId).setMenuId(menuId)));
        }
        if (CollUtil.isNotEmpty(deleteMenuIds)) {
            roleMenuMapper.deleteListByRoleIdAndMenuIds(roleId, deleteMenuIds);
        }
    }

    @Override
    public Set<Long> getRoleMenuListByRoleId(Long roleId) {
        return CollectionUtils.convertSet(roleMenuMapper.selectListByRoleId(roleId), RoleMenuDO::getMenuId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRole(Long userId, Collection<Long> roleIds) {
        Set<Long> dbRoleIds = getUserRoleIdListByUserId(userId);
        Set<Long> targetRoleIds = roleIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(roleIds);

        Collection<Long> createRoleIds = CollUtil.subtract(targetRoleIds, dbRoleIds);
        Collection<Long> deleteRoleIds = CollUtil.subtract(dbRoleIds, targetRoleIds);
        if (CollUtil.isNotEmpty(createRoleIds)) {
            userRoleMapper.insertBatch(CollectionUtils.convertList(createRoleIds,
                    roleId -> new UserRoleDO().setUserId(userId).setRoleId(roleId)));
        }
        if (CollUtil.isNotEmpty(deleteRoleIds)) {
            userRoleMapper.deleteListByUserIdAndRoleIds(userId, deleteRoleIds);
        }
    }

    @Override
    public Set<Long> getUserRoleIdListByUserId(Long userId) {
        return CollectionUtils.convertSet(userRoleMapper.selectListByUserId(userId), UserRoleDO::getRoleId);
    }
}
