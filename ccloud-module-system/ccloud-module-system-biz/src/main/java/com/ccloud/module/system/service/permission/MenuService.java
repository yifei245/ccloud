package com.ccloud.module.system.service.permission;

import com.ccloud.module.system.dal.dataobject.permission.MenuDO;

import java.util.Collection;
import java.util.List;

/**
 * 菜单 Service
 */
public interface MenuService {

    MenuDO getMenu(Long id);

    List<MenuDO> getMenuList();

    List<MenuDO> getMenuList(Collection<Long> ids);

    /**
     * @return 权限标识对应的<b>开启</b>菜单编号；菜单停用即视为该权限不可用
     */
    List<Long> getMenuIdListByPermissionFromCache(String permission);
}
