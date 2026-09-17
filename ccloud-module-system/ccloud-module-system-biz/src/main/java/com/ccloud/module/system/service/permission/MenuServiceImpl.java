package com.ccloud.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.module.system.dal.dataobject.permission.MenuDO;
import com.ccloud.module.system.dal.mysql.permission.MenuMapper;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.ccloud.module.system.dal.redis.RedisKeyConstants.PERMISSION_MENU_ID_LIST;

@Service
@Validated
public class MenuServiceImpl implements MenuService {

    @Resource
    private MenuMapper menuMapper;

    @Override
    public MenuDO getMenu(Long id) {
        return menuMapper.selectById(id);
    }

    @Override
    public List<MenuDO> getMenuList() {
        return menuMapper.selectList();
    }

    @Override
    public List<MenuDO> getMenuList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return menuMapper.selectByIds(ids);
    }

    @Override
    @Cacheable(value = PERMISSION_MENU_ID_LIST, key = "#permission")
    public List<Long> getMenuIdListByPermissionFromCache(String permission) {
        List<MenuDO> menus = menuMapper.selectListByPermission(permission);
        return CollectionUtils.convertList(menus, MenuDO::getId, menu -> CommonStatusEnum.isEnable(menu.getStatus()));
    }
}
